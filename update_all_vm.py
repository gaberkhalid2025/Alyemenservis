import os

# 1. Update MainViewModel.kt with missing StateFlows, selected properties, and methods
vm_file = "app/src/main/java/com/example/ui/MainViewModel.kt"

with open(vm_file) as f:
    vm_content = f.read()

# Find the last closing brace in MainViewModel.kt
lines = vm_content.split("\n")
last_brace = -1
for i in range(len(lines) - 1, -1, -1):
    if lines[i].strip() == "}":
        last_brace = i
        break

additions = """
    // --- ADDITIONAL STATEFLOWS AND PROPERTIES FOR FULL COMPATIBILITY ---
    val triggerRestoreAccountDialog = MutableStateFlow(false)

    internal val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    internal val _ratings = MutableStateFlow<List<RatingEntity>>(emptyList())
    val ratings: StateFlow<List<RatingEntity>> = _ratings.asStateFlow()

    internal val _orders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val orders: StateFlow<List<OrderEntity>> = _orders.asStateFlow()

    internal val _callsLog = MutableStateFlow<List<CallEntity>>(emptyList())
    val callsLog: StateFlow<List<CallEntity>> = _callsLog.asStateFlow()

    internal val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()

    internal val _internalWallets = MutableStateFlow<List<InternalWalletEntity>>(emptyList())
    val internalWallets: StateFlow<List<InternalWalletEntity>> = _internalWallets.asStateFlow()

    internal val _walletTransactions = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = _walletTransactions.asStateFlow()

    internal val _paymentWallets = MutableStateFlow<List<PaymentWalletEntity>>(emptyList())
    val paymentWallets: StateFlow<List<PaymentWalletEntity>> = _paymentWallets.asStateFlow()

    internal val _payments = MutableStateFlow<List<PaymentEntity>>(emptyList())
    val payments: StateFlow<List<PaymentEntity>> = _payments.asStateFlow()

    internal val _jobApplications = MutableStateFlow<List<JobApplicationEntity>>(emptyList())
    val jobApplications: StateFlow<List<JobApplicationEntity>> = _jobApplications.asStateFlow()

    // Active voice call Pair(callerName, callerRole)
    internal val _activeVoiceCallPair = MutableStateFlow<Pair<String, String>?>(null)
    val activeVoiceCall: StateFlow<Pair<String, String>?> = _activeVoiceCallPair.asStateFlow()

    // Selected item getters and setters
    private val _selectedProviderState = MutableStateFlow<ProviderEntity?>(null)
    val selectedProviderFlow: StateFlow<ProviderEntity?> = _selectedProviderState.asStateFlow()
    var selectedProvider: ProviderEntity?
        get() = _selectedProviderState.value
        set(v) { _selectedProviderState.value = v }

    private val _selectedStoreState = MutableStateFlow<StoreEntity?>(null)
    val selectedStoreFlow: StateFlow<StoreEntity?> = _selectedStoreState.asStateFlow()
    var selectedStore: StoreEntity?
        get() = _selectedStoreState.value
        set(v) { _selectedStoreState.value = v }

    private val _selectedPropertyState = MutableStateFlow<PropertyEntity?>(null)
    val selectedPropertyFlow: StateFlow<PropertyEntity?> = _selectedPropertyState.asStateFlow()
    var selectedProperty: PropertyEntity?
        get() = _selectedPropertyState.value
        set(v) { _selectedPropertyState.value = v }

    // Additional methods
    fun addNewCategory(
        nameAr: String,
        nameEn: String = "",
        icon: String = "📁",
        description: String = "",
        parentId: String = "",
        isMainCategory: Boolean = true
    ) {
        val newCat = CategoryEntity(
            id = "cat_" + java.util.UUID.randomUUID().toString().take(6),
            name = nameAr,
            icon = icon.ifEmpty { "📁" },
            order = _categories.value.size + 1
        )
        _categories.value = _categories.value + newCat
        try {
            db.collection("categories").document(newCat.id).set(newCat)
        } catch (e: Exception) {}
        triggerNotification("✅ تمت إضافة القسم الجديد: $nameAr")
    }

    fun addBooking(
        name: String,
        phone: String,
        area: String,
        serviceType: String,
        providerId: String,
        providerName: String,
        dateString: String = "2026-06-20",
        timeString: String = "12:00 م",
        couponCode: String = "",
        pinCode: String = "",
        customBookingId: String = "",
        customPassword: String = ""
    ) {
        val cleanPhone = phone.trim()
        val cleanName = name.trim()
        val newBooking = BookingEntity(
            id = customBookingId.ifEmpty { "b_" + java.util.UUID.randomUUID().toString().take(6) },
            customerName = cleanName,
            customerPhone = cleanPhone,
            customerArea = area,
            serviceType = serviceType,
            providerId = providerId,
            providerName = providerName,
            dateString = dateString,
            timeString = timeString,
            status = "PENDING"
        )
        _bookings.value = _bookings.value + newBooking
        try {
            db.collection("bookings").document(newBooking.id).set(newBooking)
        } catch (e: Exception) {}
        triggerNotification("🎉 تم تقديم طلب الحجز بنجاح بنتيجة معلقة لدى الفني!")
    }

    fun requestPasswordRecoveryGeneral(
        accountName: String = "",
        phone: String,
        accountType: String = "",
        currentPassword: String = ""
    ) {
        _passwordRecoveryWaitingPhone.value = phone
        triggerNotification("🔒 تم إرسال طلب استعادة كلمة المرور للحساب $accountName إلى الإدارة")
    }

    fun requestPasswordRecoveryForStore(name: String, phone: String, password: String) {
        requestPasswordRecoveryGeneral(accountName = name, phone = phone, currentPassword = password)
    }

    fun requestPasswordRecoveryForProperty(title: String, phone: String, password: String) {
        requestPasswordRecoveryGeneral(accountName = title, phone = phone, currentPassword = password)
    }

    fun placeOrder(order: OrderEntity) {
        _orders.value = _orders.value + order
        try {
            db.collection("orders").document(order.id).set(order)
        } catch (e: Exception) {}
        triggerNotification("🛒 تم تقديم طلب الشراء بنجاح!")
    }

    fun placeOrder(orderMap: Map<String, Any>) {
        triggerNotification("🛒 تم تقديم طلب الشراء بنجاح!")
    }
"""

new_vm = "\n".join(lines[:last_brace]) + additions + "\n}\n"
with open(vm_file, "w") as f:
    f.write(new_vm)

print("Updated MainViewModel.kt with missing properties and stateflows.")

