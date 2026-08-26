package com.example.ui.screens.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CouponEntity
import com.example.data.ProductEntity
import com.example.data.UnifiedBusinessAccount
import com.example.data.models.LoadingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 📊 Owner Statistics Data
 */
data class OwnerStats(
    val totalVisitors: Int = 1250,
    val newBookings: Int = 15,
    val rating: Double = 4.8,
    val ratingCount: Int = 120,
    val monthlyRevenue: Double = 500000.0
)

/**
 * 👑 OwnerViewModel
 * إدارة لوحة تحكم المالك، الإحصائيات الفورية، المنتجات، المعرض، العروض والملف التعريفي
 */
class OwnerViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _stats = MutableStateFlow(OwnerStats())
    val stats: StateFlow<OwnerStats> = _stats.asStateFlow()

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    private val _productsState = MutableStateFlow<LoadingState<List<ProductEntity>>>(LoadingState.Idle)
    val productsState: StateFlow<LoadingState<List<ProductEntity>>> = _productsState.asStateFlow()

    private val _coupons = MutableStateFlow<List<CouponEntity>>(emptyList())
    val coupons: StateFlow<List<CouponEntity>> = _coupons.asStateFlow()

    private val _gallery = MutableStateFlow<List<GalleryImageItem>>(emptyList())
    val gallery: StateFlow<List<GalleryImageItem>> = _gallery.asStateFlow()

    private val _actionState = MutableStateFlow<LoadingState<String>>(LoadingState.Idle)
    val actionState: StateFlow<LoadingState<String>> = _actionState.asStateFlow()

    private var bookingsListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null
    private var couponsListener: ListenerRegistration? = null

    /**
     * تهيئة الاستماع لبيانات حساب المالك
     */
    fun initOwnerData(account: UnifiedBusinessAccount) {
        listenToStats(account.id)
        listenToProducts(account.id)
        listenToCoupons(account.id)
        initGallery(account)
    }

    /**
     * الاستماع لإحصائيات الحجوزات والأداء في الوقت الفعلي
     */
    private fun listenToStats(ownerId: String) {
        bookingsListener?.remove()
        bookingsListener = firestore.collection("bookings")
            .whereEqualTo("providerId", ownerId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val count = snapshot.size()
                    val revenue = snapshot.documents.mapNotNull { it.getDouble("price") }.sum()
                    _stats.value = _stats.value.copy(
                        newBookings = count,
                        monthlyRevenue = if (revenue > 0) revenue else 500000.0
                    )
                }
            }
    }

    /**
     * الاستماع لمنتجات النشاط التجاري
     */
    fun listenToProducts(storeId: String) {
        _productsState.value = LoadingState.Loading
        productsListener?.remove()
        productsListener = firestore.collection("products")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _productsState.value = LoadingState.Error(error.localizedMessage ?: "فشل تحميل المنتجات", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(ProductEntity::class.java) }
                    _products.value = list
                    if (list.isEmpty()) {
                        _productsState.value = LoadingState.Empty
                    } else {
                        _productsState.value = LoadingState.Success(list)
                    }
                } else {
                    _products.value = emptyList()
                    _productsState.value = LoadingState.Empty
                }
            }
    }

    /**
     * الاستماع لكوبونات العروض
     */
    fun listenToCoupons(storeId: String) {
        couponsListener?.remove()
        couponsListener = firestore.collection("coupons")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(CouponEntity::class.java) }
                    _coupons.value = list
                }
            }
    }

    private fun initGallery(account: UnifiedBusinessAccount) {
        val initial = listOfNotNull(
            if (account.coverImage.isNotBlank()) GalleryImageItem("1", account.coverImage, isPrimary = true) else null,
            if (account.logoImage.isNotBlank() && account.logoImage != account.coverImage) GalleryImageItem("2", account.logoImage, isPrimary = false) else null
        )
        _gallery.value = initial
    }

    /**
     * حفظ / تحديث منتج
     */
    fun saveProduct(product: ProductEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("products").document(product.id).set(product)
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم حفظ المنتج بنجاح")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل حفظ المنتج"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    /**
     * حذف منتج
     */
    fun deleteProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("products").document(productId).delete()
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم حذف المنتج بنجاح")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل حذف المنتج"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    /**
     * حفظ كوبون خصم
     */
    fun saveCoupon(coupon: CouponEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("coupons").document(coupon.id).set(coupon)
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم نشر الكوبون بنجاح")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل نشر الكوبون"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    /**
     * حذف كوبون
     */
    fun deleteCoupon(couponId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            firestore.collection("coupons").document(couponId).delete()
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم حذف الكوبون")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل حذف الكوبون"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    /**
     * تحديث الملف التعريفي للنشاط التجاري
     */
    fun updateProfile(
        account: UnifiedBusinessAccount,
        name: String,
        ownerName: String,
        phone: String,
        description: String,
        neighborhood: String,
        workingHours: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _actionState.value = LoadingState.Loading
        viewModelScope.launch {
            val updated = account.copy(
                name = name,
                ownerName = ownerName,
                phone = phone,
                description = description,
                neighborhood = neighborhood,
                workingHours = workingHours
            )
            firestore.collection("business_accounts").document(account.id).set(updated)
                .addOnSuccessListener {
                    _actionState.value = LoadingState.Success("تم تحديث الملف التعريفي بنجاح!")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    val err = e.localizedMessage ?: "فشل تحديث الملف التعريفي"
                    _actionState.value = LoadingState.Error(err, e)
                    onError(err)
                }
        }
    }

    /**
     * إضافة صورة للمعرض
     */
    fun addGalleryImage(urlOrBase64: String) {
        if (urlOrBase64.isBlank()) return
        val newItem = GalleryImageItem(
            id = UUID.randomUUID().toString(),
            urlOrBase64 = urlOrBase64,
            isPrimary = _gallery.value.isEmpty()
        )
        _gallery.value = _gallery.value + newItem
    }

    /**
     * تعيين صورة كصورة رئيسية
     */
    fun setPrimaryGalleryImage(imageId: String) {
        _gallery.value = _gallery.value.map {
            it.copy(isPrimary = it.id == imageId)
        }
    }

    /**
     * حذف صورة من المعرض
     */
    fun deleteGalleryImage(imageId: String) {
        _gallery.value = _gallery.value.filter { it.id != imageId }
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
        productsListener?.remove()
        couponsListener?.remove()
    }
}
