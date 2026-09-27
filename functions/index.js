// functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');

// تهيئة Firebase Admin (إذا لم تكن مهيأة)
if (!admin.apps.length) {
    admin.initializeApp();
}
const db = admin.firestore();
const secretManager = new SecretManagerServiceClient();

/**
 * تهيئة Claims المالك والأدمن — مرة واحدة فقط.
 * محمية بمفتاح سري من متغيرات البيئة.
 * احذفها بعد الاستخدام.
 */
exports.initializeAdminClaims = functions.https.onRequest(async (req, res) => {
    const setupKey = req.query.key || req.headers['x-setup-key'];
    if (!setupKey || setupKey !== process.env.ADMIN_SETUP_KEY) {
        return res.status(403).json({ error: "Forbidden" });
    }
    
    const ownerEmail = process.env.OWNER_EMAIL;
    const adminEmail = process.env.ADMIN_EMAIL;
    
    if (!ownerEmail || !adminEmail) {
        return res.status(500).json({ error: "Env vars missing" });
    }
    
    try {
        const ownerUser = await admin.auth().getUserByEmail(ownerEmail);
        const adminUser = await admin.auth().getUserByEmail(adminEmail);
        
        await admin.auth().setCustomUserClaims(ownerUser.uid, {
            role: "OWNER", admin: true, isAdmin: true,
            isOwner: true, isSuperAdmin: true, registeredAt: Date.now()
        });
        
        await admin.auth().setCustomUserClaims(adminUser.uid, {
            role: "ADMIN", admin: true, isAdmin: true,
            isOwner: false, isSuperAdmin: false, registeredAt: Date.now()
        });
        
        res.status(200).json({
            success: true,
            owner: { email: ownerUser.email, uid: ownerUser.uid, role: "OWNER" },
            admin: { email: adminUser.email, uid: adminUser.uid, role: "ADMIN" },
            message: "Claims set. DELETE this function now."
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * ⚠️ يُستدعى لتعيين Custom Claim للأدمن أو المالك عبر البريد الإلكتروني (getUserByEmail)
 * يوحد كافة حقول المطالبات (admin, isAdmin, isOwner, isSuperAdmin, role) للتوافق التام بين
 * Cloud Functions و firestore.rules وتطبيق الأندرويد.
 */
exports.setAdminClaim = functions.https.onCall(async (data, context) => {
    const SUPER_ADMIN_SECRET = process.env.SUPER_ADMIN_SECRET;
    const callerClaims = context.auth?.token || {};
    const isCallerOwner = callerClaims.isSuperAdmin === true ||
                          callerClaims.isOwner === true ||
                          callerClaims.role === 'owner' ||
                          callerClaims.role === 'super_admin';

    if (!isCallerOwner && (!SUPER_ADMIN_SECRET || data.secret !== SUPER_ADMIN_SECRET)) {
        throw new functions.https.HttpsError('permission-denied', 'Invalid secret or insufficient permissions');
    }

    const email = (data.email || '').trim().toLowerCase();
    if (!email) {
        throw new functions.https.HttpsError('invalid-argument', 'Email is required');
    }

    const requestedRole = (data.role || (email === 'mah73646@gmail.com' ? 'owner' : 'admin')).toLowerCase();
    const isOwnerRole = requestedRole === 'owner' || requestedRole === 'super_admin';

    try {
        const user = await admin.auth().getUserByEmail(email);
        const claimsPayload = {
            admin: true,
            isAdmin: true,
            isOwner: isOwnerRole,
            isSuperAdmin: isOwnerRole,
            role: isOwnerRole ? 'owner' : 'admin',
            registeredAt: Date.now()
        };
        await admin.auth().setCustomUserClaims(user.uid, claimsPayload);

        await db.collection('admin_users').doc(user.uid).set({
            uid: user.uid,
            email: email,
            role: isOwnerRole ? 'OWNER' : 'ADMIN',
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        await db.collection('audit_logs').add({
            action: 'ADMIN_CLAIM_SET',
            targetEmail: email,
            targetUid: user.uid,
            assignedRole: claimsPayload.role,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });

        return {
            success: true,
            uid: user.uid,
            email: user.email,
            role: claimsPayload.role,
            claims: claimsPayload,
            creationTime: user.metadata.creationTime,
            lastSignInTime: user.metadata.lastSignInTime
        };
    } catch (error) {
        throw new functions.https.HttpsError('not-found', `User not found for email: ${email}`);
    }
});

/**
 * 🔑 المهمة 0: الإعداد التلقائي لـ Custom Claims لحسابات المالك والأدمن عبر getUserByEmail
 * يبحث تلقائياً عن:
 * - OWNER: mah73646@gmail.com
 * - ADMIN: meh777644@gmail.com
 * ويعين لهم Custom Claims الموحدة دون الحاجة لنسخ UIDs يدوياً.
 */
exports.setupInitialAdminClaims = functions.https.onCall(async (data, context) => {
    const SUPER_ADMIN_SECRET = process.env.SUPER_ADMIN_SECRET;
    const callerEmail = (context.auth?.token?.email || '').toLowerCase();
    const isAuthorizedCaller = callerEmail === 'mah73646@gmail.com' ||
                               callerEmail === 'meh777644@gmail.com' ||
                               context.auth?.token?.isSuperAdmin === true ||
                               context.auth?.token?.admin === true ||
                               (SUPER_ADMIN_SECRET && data?.secret === SUPER_ADMIN_SECRET);

    if (!isAuthorizedCaller) {
        throw new functions.https.HttpsError('permission-denied', 'Unauthorized to setup initial admin claims');
    }

    const targets = [
        {
            email: 'mah73646@gmail.com',
            role: 'owner',
            claims: {
                admin: true,
                isAdmin: true,
                isOwner: true,
                isSuperAdmin: true,
                role: 'owner'
            }
        },
        {
            email: 'meh777644@gmail.com',
            role: 'admin',
            claims: {
                admin: true,
                isAdmin: true,
                isOwner: false,
                isSuperAdmin: false,
                role: 'admin'
            }
        }
    ];

    const results = [];
    for (const target of targets) {
        try {
            const userRecord = await admin.auth().getUserByEmail(target.email);
            const claimsWithTimestamp = {
                ...target.claims,
                registeredAt: Date.now()
            };
            await admin.auth().setCustomUserClaims(userRecord.uid, claimsWithTimestamp);

            await db.collection('admin_users').doc(userRecord.uid).set({
                uid: userRecord.uid,
                email: target.email,
                role: target.role.toUpperCase(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });

            results.push({
                email: target.email,
                role: target.role.toUpperCase(),
                uid: userRecord.uid,
                creationTime: userRecord.metadata.creationTime,
                lastSignInTime: userRecord.metadata.lastSignInTime,
                status: 'CLAIMS_SET_SUCCESSFULLY'
            });
        } catch (err) {
            results.push({
                email: target.email,
                role: target.role.toUpperCase(),
                uid: null,
                status: 'USER_NOT_FOUND_IN_AUTH',
                error: err.message
            });
        }
    }

    await db.collection('audit_logs').add({
        action: 'INITIAL_ADMIN_CLAIMS_SETUP',
        results: results,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, accounts: results };
});

/**
 * 🔐 التحقق الآمن من تسجيل دخول الأدمن
 * - Rate limiting: 5 محاولات / 15 دقيقة
 * - التحقق من Custom Claims (مع التعيين التلقائي عند أول دخول لحساب المالك/الأدمن المعتمد)
 * - Audit logging لكل محاولة
 */
exports.verifyAdminLogin = functions.https.onCall(async (data, context) => {
    const { email, password } = data;
    const ip = context.rawRequest?.ip || 'unknown';
    
    if (!email || !password) {
        throw new functions.https.HttpsError('invalid-argument', 'Email and password required');
    }
    
    const normalizedEmail = email.trim().toLowerCase();

    // Rate limiting: 5 محاولات في 15 دقيقة
    const attemptRef = db.collection('security').doc(`login_${ip}`);
    const attemptDoc = await attemptRef.get();
    const attemptData = attemptDoc.data() || { 
        count: 0, 
        firstAttempt: Date.now() 
    };
    
    // إعادة تعيين العداد بعد 15 دقيقة
    if (Date.now() - attemptData.firstAttempt > 15 * 60 * 1000) {
        attemptData.count = 0;
        attemptData.firstAttempt = Date.now();
    }
    
    if (attemptData.count >= 5) {
        const remainingMs = 15 * 60 * 1000 - (Date.now() - attemptData.firstAttempt);
        const remainingMin = Math.ceil(remainingMs / 60000);
        throw new functions.https.HttpsError(
            'resource-exhausted',
            `محاولات كثيرة. الرجاء المحاولة بعد ${remainingMin} دقيقة`
        );
    }
    
    try {
        // التحقق عبر Firebase Auth REST API
        const apiKey = process.env.FIREBASE_API_KEY || functions.config().auth?.api_key || '';
        const response = await fetch(
            `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`,
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: normalizedEmail,
                    password,
                    returnSecureToken: true
                })
            }
        );
        
        if (!response.ok) {
            // زيادة عداد المحاولات الفاشلة
            attemptData.count += 1;
            await attemptRef.set(attemptData, { merge: true });
            
            await db.collection('audit_logs').add({
                action: 'ADMIN_LOGIN_FAILED',
                email: normalizedEmail,
                ip: ip,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            
            throw new functions.https.HttpsError('unauthenticated', 'بيانات غير صحيحة');
        }
        
        const authData = await response.json();
        const user = await admin.auth().getUser(authData.localId);
        let claims = user.customClaims || {};

        // التعيين التلقائي لـ Custom Claims إذا كان البريد هو بريد المالك أو الأدمن الرسمي ولم تُضبط بعد
        if (!claims.isAdmin && !claims.admin) {
            if (normalizedEmail === 'mah73646@gmail.com') {
                claims = { admin: true, isAdmin: true, isOwner: true, isSuperAdmin: true, role: 'owner', registeredAt: Date.now() };
                await admin.auth().setCustomUserClaims(user.uid, claims);
            } else if (normalizedEmail === 'meh777644@gmail.com') {
                claims = { admin: true, isAdmin: true, isOwner: false, isSuperAdmin: false, role: 'admin', registeredAt: Date.now() };
                await admin.auth().setCustomUserClaims(user.uid, claims);
            }
        }
        
        const hasAdminAccess = claims.isAdmin === true ||
                               claims.admin === true ||
                               claims.isSuperAdmin === true ||
                               claims.isOwner === true ||
                               ['admin', 'owner', 'super_admin'].includes((claims.role || '').toLowerCase());

        if (!hasAdminAccess) {
            throw new functions.https.HttpsError('permission-denied', 'ليست لديك صلاحيات الأدمن');
        }
        
        // إعادة تعيين العداد عند النجاح
        await attemptRef.delete();
        
        const isSuperAdmin = claims.isSuperAdmin === true || claims.isOwner === true || (claims.role || '').toLowerCase() === 'owner';

        await db.collection('audit_logs').add({
            action: 'ADMIN_LOGIN_SUCCESS',
            email: normalizedEmail,
            uid: user.uid,
            role: isSuperAdmin ? 'OWNER' : 'ADMIN',
            ip: ip,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        
        return {
            success: true,
            idToken: authData.idToken,
            refreshToken: authData.refreshToken,
            expiresIn: authData.expiresIn,
            uid: user.uid,
            isSuperAdmin: isSuperAdmin,
            role: isSuperAdmin ? 'OWNER' : 'ADMIN'
        };
        
    } catch (error) {
        if (error.code) throw error;
        throw new functions.https.HttpsError('internal', 'فشل تسجيل الدخول');
    }
});

/**
 * 📬 إرسال إشعار للأدمن عند طلب استعادة كلمة مرور
 * يُنفّذ تلقائياً عند إضافة مستند جديد في password_recovery_requests
 */
exports.onPasswordRecoveryRequest = functions.firestore
    .document('password_recovery_requests/{requestId}')
    .onCreate(async (snap, context) => {
        const request = snap.data();
        const { phone, name, accountType } = request;
        const requestId = context.params.requestId;
        
        console.log(`📬 Password recovery request: ${requestId}`);
        
        try {
            // 1. جلب FCM tokens للأدمن
            const adminTokens = await getAdminFcmTokens();
            
            if (adminTokens.length === 0) {
                console.warn('⚠️ No admin FCM tokens');
                await snap.ref.update({
                    status: 'NOTIFIED_NO_TOKENS',
                    notifiedAt: admin.firestore.FieldValue.serverTimestamp()
                });
                return { success: false, reason: 'no_tokens' };
            }
            
            // 2. إرسال FCM للأدمن
            const message = {
                tokens: adminTokens,
                notification: {
                    title: '🔑 طلب استعادة كلمة مرور',
                    body: `${accountType} - ${name} (${phone})`
                },
                data: {
                    type: 'PASSWORD_RECOVERY',
                    requestId: requestId,
                    phone: phone || '',
                    name: name || '',
                    accountType: accountType || 'حساب',
                    priority: 'HIGH',
                    clickAction: 'OPEN_PASSWORD_RECOVERY_PANEL'
                },
                android: {
                    priority: 'high',
                    ttl: 60 * 60 * 1000,
                    notification: {
                        channelId: 'admin_critical',
                        sound: 'default',
                        priority: 'max',
                        vibrateTimingsMillis: [0, 500, 200, 500],
                        color: '#EF4444',
                        icon: 'ic_notification',
                        tag: `password_recovery_${requestId}`,
                        defaultVibrateTimings: false,
                        defaultSound: true
                    }
                }
            };
            
            const fcmResult = await admin.messaging().sendMulticast(message);
            
            // 3. تنظيف التوكنات غير الصالحة
            if (fcmResult.failureCount > 0) {
                const invalidTokens = [];
                fcmResult.responses.forEach((resp, idx) => {
                    if (!resp.success && 
                        resp.error?.code === 'messaging/registration-token-not-registered') {
                        invalidTokens.push(adminTokens[idx]);
                    }
                });
                if (invalidTokens.length > 0) {
                    await removeInvalidTokens(invalidTokens);
                }
            }
            
            // 4. إضافة In-App Notification للأدمن
            await db.collection('admin_notifications').add({
                title: '🔑 طلب استعادة كلمة مرور',
                body: `${accountType} - ${name} (${phone})`,
                type: 'PASSWORD_RECOVERY',
                requestId: requestId,
                phone: phone,
                userName: name,
                accountType: accountType,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                read: false,
                priority: 'HIGH',
                requiresAction: true,
                actions: ['APPROVE', 'REJECT']
            });
            
            // 5. Audit log
            await db.collection('audit_logs').add({
                action: 'PASSWORD_RECOVERY_REQUESTED',
                actorPhone: phone,
                accountType: accountType,
                requestId: requestId,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            
            // 6. تحديث حالة الطلب
            await snap.ref.update({
                status: 'NOTIFIED',
                notifiedAt: admin.firestore.FieldValue.serverTimestamp(),
                fcmSent: fcmResult.successCount,
                fcmFailed: fcmResult.failureCount
            });
            
            console.log(`✅ Notification sent to ${fcmResult.successCount} admin(s)`);
            return { success: true, notified: fcmResult.successCount };
            
        } catch (error) {
            console.error('❌ Error:', error);
            await snap.ref.update({
                status: 'NOTIFICATION_FAILED',
                error: error.message,
                failedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            throw error;
        }
    });

/**
 * 📱 جلب FCM tokens لكل الأدمن
 */
async function getAdminFcmTokens() {
    const tokens = [];
    
    try {
        const listUsersResult = await admin.auth().listUsers(1000);
        const adminUids = listUsersResult.users
            .filter(user => {
                const c = user.customClaims || {};
                return c.isAdmin === true || c.admin === true || c.isOwner === true || c.isSuperAdmin === true ||
                       ['admin', 'owner', 'super_admin'].includes((c.role || '').toLowerCase());
            })
            .map(user => user.uid);
        
        if (adminUids.length === 0) return [];
        
        // جلب التوكنات لكل أدمن
        for (const uid of adminUids) {
            const tokensSnapshot = await db.collection('fcm_tokens')
                .where('userId', '==', uid)
                .where('isActive', '==', true)
                .get();
            
            tokensSnapshot.forEach(doc => {
                const token = doc.data().token;
                if (token) tokens.push(token);
            });
        }
    } catch (e) {
        console.error('Error getting admin tokens:', e);
    }
    
    return [...new Set(tokens)];
}

/**
 * 🧹 حذف التوكنات غير الصالحة
 */
async function removeInvalidTokens(tokens) {
    const batch = db.batch();
    for (const token of tokens) {
        const snapshot = await db.collection('fcm_tokens')
            .where('token', '==', token)
            .get();
        snapshot.forEach(doc => batch.delete(doc.ref));
    }
    await batch.commit();
}

/**
 * 🔑 جلب مفتاح API بشكل آمن
 * - يتطلب صلاحيات الأدمن
 * - يسجل كل عملية وصول
 */
exports.getApiKey = functions.https.onCall(async (data, context) => {
    // التحقق من صلاحيات الأدمن الموحدة
    const token = context.auth?.token || {};
    const isAdmin = token.isAdmin === true || token.admin === true || token.isOwner === true || token.isSuperAdmin === true ||
                    ['admin', 'owner', 'super_admin'].includes((token.role || '').toLowerCase());
    if (!isAdmin) {
        throw new functions.https.HttpsError('permission-denied', 'Admin only');
    }
    
    const { keyName } = data;
    
    if (!keyName || typeof keyName !== 'string') {
        throw new functions.https.HttpsError('invalid-argument', 'keyName required');
    }
    
    const secretName = `yemen_app_${keyName.toLowerCase().replace(/_/g, '-')}`;
    
    try {
        const [version] = await secretManager.accessSecretVersion({
            name: `projects/${process.env.GCLOUD_PROJECT}/secrets/${secretName}/versions/latest`
        });
        
        // Audit log (بدون القيمة الفعلية!)
        await db.collection('audit_logs').add({
            action: 'API_KEY_ACCESSED',
            keyName: keyName,
            adminId: context.auth.uid,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        
        return {
            success: true,
            value: version.payload.data.toString('utf8')
        };
    } catch (error) {
        console.error(`Error accessing ${secretName}:`, error);
        throw new functions.https.HttpsError(
            'not-found',
            `Key not found: ${keyName}`
        );
    }
});

/**
 * 💾 حفظ مفتاح API بشكل آمن
 * - يتطلب صلاحيات الأدمن
 * - يخزّن في Secret Manager (ليس Firestore)
 */
exports.setApiKey = functions.https.onCall(async (data, context) => {
    const token = context.auth?.token || {};
    const isAdmin = token.isAdmin === true || token.admin === true || token.isOwner === true || token.isSuperAdmin === true ||
                    ['admin', 'owner', 'super_admin'].includes((token.role || '').toLowerCase());
    if (!isAdmin) {
        throw new functions.https.HttpsError('permission-denied', 'Admin only');
    }
    
    const { keyName, value } = data;
    
    if (!keyName || !value) {
        throw new functions.https.HttpsError('invalid-argument', 'keyName and value required');
    }
    
    const secretName = `yemen_app_${keyName.toLowerCase().replace(/_/g, '-')}`;
    
    try {
        // محاولة الحصول على السر الموجود
        try {
            await secretManager.getSecret({
                name: `projects/${process.env.GCLOUD_PROJECT}/secrets/${secretName}`
            });
            // موجود — نضيف نسخة جديدة
        } catch (e) {
            // غير موجود — ننشئه
            await secretManager.createSecret({
                parent: `projects/${process.env.GCLOUD_PROJECT}`,
                secretId: secretName,
                secret: {
                    replication: { automatic: {} }
                }
            });
        }
        
        // إضافة نسخة جديدة
        await secretManager.addSecretVersion({
            parent: `projects/${process.env.GCLOUD_PROJECT}/secrets/${secretName}`,
            payload: {
                data: Buffer.from(value).toString('base64')
            }
        });
        
        // Audit log
        await db.collection('audit_logs').add({
            action: 'API_KEY_UPDATED',
            keyName: keyName,
            adminId: context.auth.uid,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        
        return { success: true };
    } catch (error) {
        console.error(`Error saving ${secretName}:`, error);
        throw new functions.https.HttpsError('internal', 'Failed to save key');
    }
});
