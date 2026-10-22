const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');

let testEnv;

beforeAll(async () => {
    testEnv = await initializeTestEnvironment({
        projectId: 'al-yemen-services',
        firestore: {
            rules: require('fs').readFileSync('firestore.rules', 'utf8'),
        },
    });
});

afterAll(async () => {
    await testEnv.cleanup();
});

beforeEach(async () => {
    await testEnv.clearFirestore();
});

describe('firestore.rules', () => {
    test('مستخدم غير مصادق لا يقرأ jobs', async () => {
        const db = testEnv.unauthenticatedContext().firestore();
        await assertFails(db.collection('jobs').doc('test').get());
    });

    test('مستخدم عادي لا يقرأ password_resets لغيره', async () => {
        const db = testEnv.authenticatedContext('user1').firestore();
        await testEnv.withSecurityRulesDisabled(async (ctx) => {
            await ctx.firestore().collection('password_resets').doc('r1').set({
                uid: 'user2', token: 'x'
            });
        });
        await assertFails(db.collection('password_resets').doc('r1').get());
    });

    test('أدمن يقرأ password_resets', async () => {
        const db = testEnv.authenticatedContext('admin1', {
            role: 'ADMIN', isAdmin: true
        }).firestore();
        await assertSucceeds(db.collection('password_resets').doc('any').get());
    });

    test('أدمن يقرأ chat messages', async () => {
        const db = testEnv.authenticatedContext('admin1', {
            role: 'ADMIN'
        }).firestore();
        await assertSucceeds(db.collection('chat_channels').doc('c1').collection('messages').doc('m1').get());
    });
});
