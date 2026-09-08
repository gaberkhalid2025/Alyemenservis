import re

content = """
1: package com.example.ui
2: import androidx.compose.runtime.getValue
3: import androidx.compose.runtime.setValue
4: import androidx.compose.runtime.mutableStateOf
5: import dagger.hilt.android.lifecycle.HiltViewModel
6: import javax.inject.Inject
7: import com.example.utils.*
8: import com.example.ui.viewmodels.BaseViewModel
9: import com.example.ui.viewmodels.BookingDistributionMode
10: import com.example.ui.viewmodels.BookingFormFields
11: import com.example.ui.viewmodels.BookingStatus
12: import androidx.lifecycle.ViewModel
13: import androidx.lifecycle.viewModelScope
14: import com.example.data.*
15: import com.example.data.models.*
16: import com.example.ui.viewmodels.SettingsViewModel.CardSettings
17: import com.example.ui.viewmodels.SettingsViewModel.ChatParticipantType
18: import kotlinx.coroutines.flow.*
19: import kotlinx.coroutines.launch
20: import java.util.UUID
21: 
22: @HiltViewModel
23: class MainViewModel @Inject constructor(
24:     val authViewModel: com.example.ui.viewmodels.AuthViewModel,
25:     val homeViewModel: com.example.ui.viewmodels.HomeViewModel,
26:     val bookingViewModel: com.example.ui.viewmodels.BookingViewModel,
27:     val adminViewModel: com.example.ui.viewmodels.AdminViewModel,
28:     val settingsViewModel: com.example.ui.viewmodels.SettingsViewModel,
29:     val instantRequestViewModel: com.example.ui.viewmodels.InstantRequestViewModel,
30:     val chatRepo: com.example.data.repositories.ChatRepository
31: ) : BaseViewModel() {
32: 
33:     val preferenceHelper = com.example.ui.helpers.AppPreferenceHelper()
34:     val firestoreSeedHelper by lazy { com.example.ui.helpers.FirestoreSeedHelper(db) }
35:     val realtimeSyncHelper by lazy { com.example.ui.helpers.RealtimeSyncHelper(db) }
36:     val registrationHelper by lazy { com.example.ui.helpers.RegistrationHelper(db, auth, preferenceHelper) }
37:     val accountRecoveryHelper by lazy { com.example.ui.helpers.AccountRecoveryHelper(db, preferenceHelper) }
38: 
39:     override fun onCleared() {
40:         super.onCleared()
41:         try {
42:             realtimeSyncHelper.clearListeners()
43:         } catch (e: Exception) {
44:             e.printStackTrace()
45:         }
46:     }
47:     private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
48:     internal val _currentLanguage = MutableStateFlow("ar")
49:     val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
50:     internal val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
51:     val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()
52:     internal val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
53:     val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()
54:     internal val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
55:     val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()
56:     internal val _supervisors = MutableStateFlow<List<SupervisorEntity>>(emptyList())
57:     val supervisors: StateFlow<List<SupervisorEntity>> = _supervisors.asStateFlow()
58:     internal val _colorPalettes = MutableStateFlow<List<ColorPaletteEntity>>(emptyList())
59:     val colorPalettes: StateFlow<List<ColorPaletteEntity>> = _colorPalettes.asStateFlow()
60:     internal val _isOnline = MutableStateFlow(true)
61:     val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
62:     internal val _currentUserPoints = MutableStateFlow(0)
63:     val currentUserPoints: StateFlow<Int> = _currentUserPoints.asStateFlow()
64:     internal val _screenBackStack = MutableStateFlow<List<String>>(listOf("USER_BROWSE"))
65:     val screenBackStack: StateFlow<List<String>> = _screenBackStack.asStateFlow()
66:     val notificationViewModel = com.example.ui.screens.notifications.NotificationViewModel(this)
67:     val _currentUserId get() = authViewModel._currentUserId
68:     val currentUserId get() = authViewModel.currentUserId
69:     val _currentUserName get() = authViewModel._currentUserName
70:     val currentUserName get() = authViewModel.currentUserName
71:     val _currentUserPhone get() = authViewModel._currentUserPhone
72:     val currentUserPhone get() = authViewModel.currentUserPhone
73:     val _currentUserResidence get() = authViewModel._currentUserResidence
74:     val currentUserResidence get() = authViewModel.currentUserResidence
75:     val _adminRole get() = authViewModel._adminRole
76:     val adminRole get() = authViewModel.adminRole
77:     val _passwordRecoveryWaitingPhone get() = authViewModel._passwordRecoveryWaitingPhone
78:     val passwordRecoveryWaitingPhone get() = authViewModel.passwordRecoveryWaitingPhone
79:     val _joinRequestPhone get() = authViewModel._joinRequestPhone
80:     val joinRequestPhone get() = authViewModel.joinRequestPhone
81:     val showBackdoorDialog get() = authViewModel.showBackdoorDialog
82:     val _categories get() = homeViewModel._categories
83:     val categories get() = homeViewModel.categories
84:     val _providers get() = homeViewModel._providers
85:     val providers get() = homeViewModel.providers
86:     val _filteredProviders get() = homeViewModel._filteredProviders
87:     val filteredProviders get() = homeViewModel.filteredProviders
88:     val _banners get() = homeViewModel._banners
89:     val banners get() = homeViewModel.banners
90:     val _selectedCategoryId get() = homeViewModel._selectedCategoryId
91:     val selectedCategoryId get() = homeViewModel.selectedCategoryId
92:     val _searchQuery get() = homeViewModel._searchQuery
93:     val searchQuery get() = homeViewModel.searchQuery
94:     val _filterVipOnly get() = homeViewModel._filterVipOnly
95:     val filterVipOnly get() = homeViewModel.filterVipOnly
96:     val _filterAvailableOnly get() = homeViewModel._filterAvailableOnly
97:     val filterAvailableOnly get() = homeViewModel.filterAvailableOnly
98:     val _filterCityId get() = homeViewModel._filterCityId
99:     val filterCityId get() = homeViewModel.filterCityId
100:     val _filterNeighborhoodName get() = homeViewModel._filterNeighborhoodName
101:     val filterNeighborhoodName get() = homeViewModel.filterNeighborhoodName
102:     val _phoneOrNameFilter get() = homeViewModel._phoneOrNameFilter
103:     val phoneOrNameFilter get() = homeViewModel.phoneOrNameFilter
104:     val _bookings get() = bookingViewModel._bookings
105:     val bookings get() = bookingViewModel.bookings
106:     val _bookingFormFields = bookingViewModel._bookingFormFields
107:     val bookingFormFields = bookingViewModel.bookingFormFields
108:     val _distributionMode = bookingViewModel._distributionMode
109:     val distributionMode = bookingViewModel.distributionMode
110:     internal val _chatMessages = MutableStateFlow<List<com.example.data.ChatMessageEntity>>(emptyList())
111:     val chatMessages: StateFlow<List<com.example.data.ChatMessageEntity>> = _chatMessages.asStateFlow()
112:     internal val _chatChannels = MutableStateFlow<List<com.example.data.ChatChannelEntity>>(emptyList())
113:     val chatChannels: StateFlow<List<com.example.data.ChatChannelEntity>> = _chatChannels.asStateFlow()
114:     internal val _activeChatChannel = MutableStateFlow<com.example.data.ChatChannelEntity?>(null)
115:     val activeChatChannel: StateFlow<com.example.data.ChatChannelEntity?> = _activeChatChannel.asStateFlow()
116:     val _pendingProviders get() = adminViewModel._pendingProviders
117:     val pendingProviders get() = adminViewModel.pendingProviders
118:     val _pendingTechnicians get() = adminViewModel._pendingTechnicians
119:     val pendingTechnicians get() = adminViewModel.pendingTechnicians
120:     val _registeredUsersList get() = adminViewModel._registeredUsersList
121:     val registeredUsersList get() = adminViewModel.registeredUsersList
122:     val _registeredUsersCount get() = adminViewModel._registeredUsersCount
123:     val registeredUsersCount get() = adminViewModel.registeredUsersCount
124:     val _reports get() = adminViewModel._reports
125:     val reports get() = adminViewModel.reports
126:     val _activityLogs get() = adminViewModel._activityLogs
127:     val activityLogs get() = adminViewModel.activityLogs
128:     val _callsLog get() = adminViewModel._callsLog
129:     val callsLog get() = adminViewModel.callsLog
130:     val _coupons get() = adminViewModel._coupons
131:     val coupons get() = adminViewModel.coupons
132:     val _internalWallets get() = adminViewModel._internalWallets
133:     val internalWallets get() = adminViewModel.internalWallets
134:     val _walletTransactions get() = adminViewModel._walletTransactions
135:     val walletTransactions get() = adminViewModel.walletTransactions
136:     val _paymentWallets get() = adminViewModel._paymentWallets
137:     val paymentWallets get() = adminViewModel.paymentWallets
138:     val _payments get() = adminViewModel._payments
139:     val payments get() = adminViewModel.payments
140:     val _orders get() = adminViewModel._orders
141:     val orders get() = adminViewModel.orders
142:     val _ratings get() = adminViewModel._ratings
143:     val ratings get() = adminViewModel.ratings
144:     val _customProfileTabs get() = adminViewModel._customProfileTabs
145:     val customProfileTabs get() = adminViewModel.customProfileTabs
146:     val _stores get() = adminViewModel._stores
147:     val stores get() = adminViewModel.stores
148:     val _products get() = adminViewModel._products
149:     val products get() = adminViewModel.products
150:     val _properties get() = adminViewModel._properties
151:     val properties get() = adminViewModel.properties
152:     val _jobs get() = adminViewModel._jobs
153:     val jobs get() = adminViewModel.jobs
154:     val _jobApplications get() = adminViewModel._jobApplications
155:     val jobApplications get() = adminViewModel.jobApplications
156:     val _settings get() = settingsViewModel._settings
157:     val settings get() = settingsViewModel.settings
158:     val _cardSettings get() = settingsViewModel._cardSettings
159:     val cardSettings get() = settingsViewModel.cardSettings
160:     val _colorScheme get() = settingsViewModel._colorScheme
161:     val colorScheme get() = settingsViewModel.colorScheme
162:     val _personalColors get() = settingsViewModel._personalColors
163:     val personalColors get() = settingsViewModel.personalColors
164:     val _colorSyncStatus get() = settingsViewModel._colorSyncStatus
165:     val colorSyncStatus get() = settingsViewModel.colorSyncStatus
166:     val _colorSyncLogs get() = settingsViewModel._colorSyncLogs
167:     val colorSyncLogs get() = settingsViewModel.colorSyncLogs
168:     val _pendingConflictScheme get() = settingsViewModel._pendingConflictScheme
169:     val pendingConflictScheme get() = settingsViewModel.pendingConflictScheme
170:     val _blockedChatParticipants get() = settingsViewModel._blockedChatParticipants
171:     val blockedChatParticipants get() = settingsViewModel.blockedChatParticipants
172:     val _activeVoiceCall get() = settingsViewModel._activeVoiceCall
173:     val activeVoiceCall get() = settingsViewModel.activeVoiceCall
174:     val _instantRequests get() = instantRequestViewModel._instantRequests
175:     val instantRequests get() = instantRequestViewModel.instantRequests
176:     val _requestOffers get() = instantRequestViewModel._requestOffers
177:     val requestOffers get() = instantRequestViewModel.requestOffers
178:     val _offers get() = instantRequestViewModel._offers
179:     val offers get() = instantRequestViewModel.offers
180:     internal val _currentScreen = MutableStateFlow("USER_BROWSE")
181:     val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
182:     internal val _navigationStack = mutableListOf<String>()
183:     
184:     var selectedProvider: com.example.data.ProviderEntity? = null
185:     var selectedStore: com.example.data.StoreEntity? = null
186:     var selectedProperty: com.example.data.PropertyEntity? = null
187:     var selectedJob: com.example.data.JobEntity? = null
188:     var selectedOfferId by androidx.compose.runtime.mutableStateOf("")
189:     var selectedRequestId by androidx.compose.runtime.mutableStateOf("")
190:     var showQuickServiceDialog by androidx.compose.runtime.mutableStateOf(false)
191:     internal val _userLatitude = MutableStateFlow(15.3694)
192:     val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()
193:     internal val _userLongitude = MutableStateFlow(44.1910)
194:     val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()
195:     internal val _isGpsTrackingActive = MutableStateFlow(false)
196:     val isGpsTrackingActive: StateFlow<Boolean> = _isGpsTrackingActive.asStateFlow()
197:     internal val _isProvidersLoading = MutableStateFlow(true)
198:     val isProvidersLoading: StateFlow<Boolean> = _isProvidersLoading.asStateFlow()
199:     internal val _isChatChannelsLoading = MutableStateFlow(true)
200:     val isChatChannelsLoading: StateFlow<Boolean> = _isChatChannelsLoading.asStateFlow()
201:     internal val _cities = MutableStateFlow<List<CityEntity>>(emptyList())
202:     val cities: StateFlow<List<CityEntity>> = _cities.asStateFlow()
203:     internal val _deletedProviders = MutableStateFlow<List<ProviderEntity>>(emptyList())
204:     val deletedProviders: StateFlow<List<ProviderEntity>> = _deletedProviders.asStateFlow()
205:     internal val _isInitialized = MutableStateFlow(false)
206:     val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
207:     internal val _maxKmRadius = MutableStateFlow(10)
208:     val maxKmRadius: StateFlow<Int> = _maxKmRadius.asStateFlow()
209:     init {
210:         _stores.value = getDefaultStoresList()
211:         _properties.value = getDefaultPropertiesList()
212:     }
213:     private fun checkAndTriggerFavoriteOffersNotifications() {
214:     }
215:     fun updateUserLocation(lat: Double, lng: Double) {
216:         _userLatitude.value = lat
217:         _userLongitude.value = lng
218:     }
219:     fun startLocationUpdates() {
220:         _isGpsTrackingActive.value = true
221:         appContext?.let { ctx ->
222:             try {
223:                 val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
224:                 val loc = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
225:                     ?: lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
226:                 loc?.let {
227:                     updateUserLocation(it.latitude, it.longitude)
228:                 }
229:             } catch (e: Exception) {
230:                 android.util.Log.e("MainViewModel", "Error: ", e)
231:             }
232:         }
233:     }
234:     fun refreshData(showNotification: Boolean = false) {
235:         viewModelScope.launch {
236:             _isRefreshing.value = true
237:             _uiErrorMessage.value = null
238:             try {
239:                 setupRealtimeFirestoreListeners()
240:             } catch (e: Exception) {
241:                 _uiErrorMessage.value = "تعذر تحديث البيانات: ${e.localizedMessage}"
242:             } finally {
243:                 kotlinx.coroutines.delay(600)
244:                 _isRefreshing.value = false
245:             }
246:         }
247:     }
248:     fun updateOnlineStatus(online: Boolean) {
249:         _isOnline.value = online
250:     }
251:     fun retryConnection(context: android.content.Context) {
252:         val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
253:         if (cm != null) {
254:             val activeNetwork = cm.activeNetwork
255:             val capabilities = cm.getNetworkCapabilities(activeNetwork)
256:             val online = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
257:             _isOnline.value = online
258:             if (online) {
259:                 setupRealtimeFirestoreListeners()
260:             }
261:         }
262:     }
263:     fun updateUserFcmToken(userId: String, token: String) {
264:         if (userId.isEmpty() || userId == "guest") return
265:         try {
266:             db.collection("registered_users").document(userId).update("fcmToken", token)
267:             val cleanPhone = _currentUserPhone.value.trim().replace(" ", "").replace("+", "")
268:             if (cleanPhone.isNotEmpty()) {
269:                 db.collection("providers").document(cleanPhone).update("fcmToken", token)
270:                 db.collection("stores").document(cleanPhone).update("fcmToken", token)
271:                 db.collection("properties").document(cleanPhone).update("fcmToken", token)
272:             }
273:         } catch (e: Exception) {
274:             android.util.Log.e("MainViewModel", "Error: ", e)
275:         }
276:     }
277:     fun initializeFirestoreCollections() {
278:         val collections = listOf(
279:             "users", "pending_providers", "providers", "stores",
280:             "restaurants", "medical", "properties", "jobs",
281:             "bookings", "instant_requests", "instant_offers",
282:             "notifications", "password_reset_requests", "join_requests", "fcm_tokens"
283:         )
284:         viewModelScope.launch {
285:             collections.forEach { collection ->
286:                 try {
287:                     db.collection(collection).document("_init_")
288:                         .set(mapOf("initialized" to true))
289:                         .addOnSuccessListener {
290:                             db.collection(collection).document("_init_").delete()
291:                         }
292:                 } catch (e: Exception) {
293:                     android.util.Log.e("MainViewModel", "Error: ", e)
294:                 }
295:             }
296:         }
297:     }
298:     fun initializeUserIdentity(context: android.content.Context) {
299:         appContext = context.applicationContext
300:         authViewModel.appContext = appContext
301:         homeViewModel.appContext = appContext
302:         bookingViewModel.appContext = appContext
303:         adminViewModel.appContext = appContext
304:         settingsViewModel.appContext = appContext
305:         instantRequestViewModel.appContext = appContext
306:         bookingViewModel.getCoupons = { _coupons.value }
307:         bookingViewModel.getProviders = { _providers.value }
308:         bookingViewModel.getCurrentUserPhone = { _currentUserPhone.value }
309:         bookingViewModel.getCurrentUserName = { _currentUserName.value }
310:         bookingViewModel.getCurrentUserResidence = { _currentUserResidence.value }
311:         bookingViewModel.setCurrentUserPhone = { _currentUserPhone.value = it }
312:         bookingViewModel.setCurrentUserName = { _currentUserName.value = it }
313:         bookingViewModel.setCurrentUserResidence = { _currentUserResidence.value = it }
314:         bookingViewModel.onAddNotification = { title, message, targetType, targetValue ->
315:         }
316:         bookingViewModel.triggerNotificationCallback = { msg ->
317:         }
318:         bookingViewModel.onOpenOrCreateChatChannel = { targetId, targetType, targetName, targetPhone, targetCategory, relatedEntityId, relatedEntityType, onComplete ->
319:         }
320:         adminViewModel.getHomeViewModel = { homeViewModel }
321:         adminViewModel.getSettingsViewModel = { settingsViewModel }
322:         adminViewModel.getBookingViewModel = { bookingViewModel }
323:         adminViewModel.getInstantRequestViewModel = { instantRequestViewModel }
324:         adminViewModel.getNotifications = { _notifications }
325:         adminViewModel.onAddNotification = { title, message, targetType, targetValue ->
326:         }
327:         adminViewModel.onTriggerNotificationFull = { title, message, targetType, targetValue ->
328:         }
329:         adminViewModel.onTriggerNotification = { msg ->
330:         }
331:         adminViewModel.onApplyFilters = {  }
332:         settingsViewModel.getAuthViewModel = { authViewModel }
333:         settingsViewModel.getHomeViewModel = { homeViewModel }
334:         settingsViewModel.getBookingViewModel = { bookingViewModel }
335:         settingsViewModel.getAdminViewModel = { adminViewModel }
336:         settingsViewModel.getProviders = { _providers }
337:         settingsViewModel.getBookings = { _bookings }
338:         settingsViewModel.getCategories = { _categories }
339:         settingsViewModel.getStores = { _stores }
340:         settingsViewModel.getProperties = { _properties }
341:         settingsViewModel.getPasswordRecoveryWaitingPhone = { _passwordRecoveryWaitingPhone }
342:         settingsViewModel.setPasswordRecoveryWaitingPhone = {  }
343:         settingsViewModel.verifyAdminOrOwnerPassword = { true }
344:         settingsViewModel.triggerNotification = {  }
345:         instantRequestViewModel.triggerNotification = {  }
346:         instantRequestViewModel.addNotification = { title, message, targetType, targetValue ->
347:         }
348:         instantRequestViewModel.getOrCreateChatChannel = { providerId, providerName, customerPhone, customerName ->
349:         }
350:         try {
351:             initializeFirestoreCollections()
352:         } catch (e: Exception) {
353:             android.util.Log.e("MainViewModel", "❌ Error in initializeFirestoreCollections", e)
354:         }
355:         try {
356:             authViewModel.initializeUserIdentity(context) { savedFavs ->
357:                 _favoriteIds.value = savedFavs
358:                 checkAndTriggerFavoriteOffersNotifications()
359:             }
360:         } catch (e: Exception) {
361:             android.util.Log.e("MainViewModel", "❌ Error in AuthViewModel initialization", e)
362:         }
363:         try {
364:             setupRealtimeFirestoreListeners()
365:         } catch (e: Exception) {
366:             android.util.Log.e("MainViewModel", "❌ Error in setting up realtime listeners", e)
367:         }
368:         try {
369:             settingsViewModel.loadCardSettings()
370:         } catch (e: Exception) {
371:             android.util.Log.e("MainViewModel", "❌ Error in SettingsViewModel loadCardSettings", e)
372:         }
373:         try {
374:             adminViewModel.loadPendingTechnicians()
375:         } catch (e: Exception) {
376:             android.util.Log.e("MainViewModel", "❌ Error in AdminViewModel loadPendingTechnicians", e)
377:         }
378:         try {
379:             seedFirestoreIfEmpty()
380:         } catch (e: Exception) {
381:             android.util.Log.e("MainViewModel", "❌ Error in seedFirestoreIfEmpty", e)
382:         }
383:         viewModelScope.launch {
384:             kotlinx.coroutines.delay(2200)
385:             _isInitialized.value = true
386:         }
387:     }
388:     fun setupRealtimeFirestoreListeners() {
389:         realtimeSyncHelper.clearListeners()
390:         realtimeSyncHelper.setupRealtimeFirestoreListeners(this)
391:     }
392:     fun seedFirestoreIfEmpty() {
393:         firestoreSeedHelper.seedFirestoreIfEmpty()
394:     }
395:     override fun getDefaultStoresList(): List<com.example.data.StoreEntity> {
396:         return emptyList()
397:     }
398:     override fun getDefaultPropertiesList(): List<com.example.data.PropertyEntity> {
399:         return emptyList()
400:     }
401:     override suspend fun uploadImageStringOrUri(
402:         context: android.content.Context,
403:         input: String,
404:         storagePath: String,
405:         maxSizeBytes: Long
406:     ): String {
407:         return registrationHelper.uploadImageStringOrUri(context, input, storagePath, maxSizeBytes)
408:     }
409:     override fun getAuthEmailForPhone(phone: String): String = authViewModel.getAuthEmailForPhone(phone)
410: 
411:     var lastNotifMsg: String = ""
412:     var lastNotifTime: Long = 0L
413:     val triggerRestoreAccountDialog = MutableStateFlow(false)
414:     var targetChatChannelId by mutableStateOf<String?>(null)
415: }
"""

text = re.sub(r'^\d+:\s', '', content, flags=re.MULTILINE)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(text)

