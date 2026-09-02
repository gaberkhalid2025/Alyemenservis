package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CouponEntity
import com.example.data.UnifiedBusinessAccount
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class OfferModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val discountPercent: Int,
    val validUntil: String,
    val isActive: Boolean = true
)

/**
 * 🏷️ Modular Tab: Offers, Discounts, Coupons & Inventory (إدارة العروض والكوبونات والمخزون)
 */
@Composable
fun TabOffersCoupons(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showAddOfferDialog by remember { mutableStateOf(false) }
    var showAddCouponDialog by remember { mutableStateOf(false) }
    var showBulkPricingDialog by remember { mutableStateOf(false) }

    // Parse offers from store or provider specialOffersJson
    val rawJson = account.rawStore?.specialOffersJson ?: account.rawProvider?.specialOffersJson ?: ""
    var offersList by remember(rawJson) {
        val list = mutableListOf<OfferModel>()
        if (rawJson.isNotBlank()) {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        OfferModel(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            discountPercent = obj.optInt("discountPercent", 10),
                            validUntil = obj.optString("validUntil", "2026-12-31"),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        mutableStateOf(list.toList())
    }

    // Coupons from ViewModel / Firestore
    val allCoupons by viewModel.coupons.collectAsState()

    // Products for Inventory & Pricing
    val allProducts by viewModel.products.collectAsState()
    val myProducts = remember(allProducts, account.id, account.phone) {
        allProducts.filter { (it.storeId == account.id || it.storeId == account.phone) && !it.isDeleted }
    }

    fun saveOffersToFirestore(updated: List<OfferModel>) {
        offersList = updated
        val array = JSONArray()
        updated.forEach { o ->
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("title", o.title)
            obj.put("description", o.description)
            obj.put("discountPercent", o.discountPercent)
            obj.put("validUntil", o.validUntil)
            obj.put("isActive", o.isActive)
            array.put(obj)
        }
        val jsonString = array.toString()
        if (account.rawStore != null) {
            viewModel.saveStore(account.rawStore.copy(specialOffersJson = jsonString))
        } else if (account.rawProvider != null) {
            viewModel.updateProviderEntity(account.rawProvider.copy(specialOffersJson = jsonString))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-tabs row: Offers, Coupons, Inventory
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val subTabs = listOf(
                Pair("🏷️", "العروض (${offersList.size})"),
                Pair("🎟️", "الكوبونات (${allCoupons.size})"),
                Pair("📦", "المخزون والأسعار (${myProducts.size})")
            )
            subTabs.forEachIndexed { index, pair ->
                val isSelected = selectedSubTab == index
                Button(
                    onClick = { selectedSubTab = index },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) themeColors.accent else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pair.first} ${pair.second}",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                // OFFERS LIST
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏷️ العروض والخصومات الحالية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = { showAddOfferDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة عرض ➕", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (offersList.isEmpty()) {
                    UnifiedEmptyState(
                        icon = "🏷️",
                        title = "لا توجد عروض ترويجية نشطة",
                        description = "أضف عروضاً وخصومات لجذب المزيد من الزبائن وزيادة حجم المبيعات.",
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(offersList) { offer ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(offer.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "خصم ${offer.discountPercent}%",
                                                    color = Color(0xFF10B981),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(offer.description, fontSize = 10.5.sp, color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("🗓️ صالح حتى: ${offer.validUntil}", fontSize = 9.5.sp, color = Color.Gray)
                                    }

                                    IconButton(
                                        onClick = {
                                            val updated = offersList.filter { it.id != offer.id }
                                            saveOffersToFirestore(updated)
                                            Toast.makeText(context, "🗑️ تم حذف العرض بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // COUPONS LIST
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎟️ كوبونات وأكواد الخصم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = { showAddCouponDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة كوبون ➕", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (allCoupons.isEmpty()) {
                    UnifiedEmptyState(
                        icon = "🎟️",
                        title = "لا توجد كوبونات خصم",
                        description = "أنشئ كوبونات ترويجية خاصة بمتجرك ليستخدمها العملاء عند الطلب والحجز.",
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(allCoupons) { coupon ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("كود: ${coupon.code}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = if (coupon.status == "ACTIVE") Color(0xFF10B981) else Color.Gray,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (coupon.status == "ACTIVE") "نشط ✓" else "معطل",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("خصم: ${coupon.discountPercentage}% • استخدام: ${coupon.usedCount}/${coupon.maxUsageCount}", fontSize = 10.5.sp, color = Color.White)
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                val newStatus = if (coupon.status == "ACTIVE") "INACTIVE" else "ACTIVE"
                                                viewModel.saveCoupon(coupon.copy(status = newStatus))
                                            }
                                        ) {
                                            Icon(
                                                if (coupon.status == "ACTIVE") Icons.Default.CheckCircle else Icons.Default.Close,
                                                contentDescription = "تبديل الحالة",
                                                tint = if (coupon.status == "ACTIVE") Color(0xFF10B981) else Color.Gray
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCoupon(coupon.id)
                                                Toast.makeText(context, "🗑️ تم حذف الكوبون بنجاح", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // INVENTORY & PRICING
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📦 إدارة توفر المخزون والأسعار", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                    Button(
                        onClick = { showBulkPricingDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل جماعي للأسعار ⚡", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (myProducts.isEmpty()) {
                    UnifiedEmptyState(
                        icon = "📦",
                        title = "لا توجد منتجات مسجلة بالمخزون",
                        description = "أضف منتجات وخدمات في تبويب (المنتجات والخدمات) لإدارتها وتحديد حالة توفرها.",
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(myProducts) { prod ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("السعر الحالي: ${prod.price.toInt()} ر.ي", fontSize = 11.sp, color = themeColors.accent)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (prod.isAvailable) "متوفر بالمخزون ✓" else "نفد من المخزون ❌",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (prod.isAvailable) Color(0xFF10B981) else Color(0xFFEF5350)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = prod.isAvailable,
                                            onCheckedChange = { checked ->
                                                viewModel.saveProduct(prod.copy(isAvailable = checked))
                                                Toast.makeText(context, if (checked) "✅ تم تفعيل توفر المنتج" else "⚠️ تم تعيين المنتج كـ نفد من المخزون", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF10B981)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Offer Dialog
    if (showAddOfferDialog) {
        var offerTitle by remember { mutableStateOf("") }
        var offerDesc by remember { mutableStateOf("") }
        var offerDiscount by remember { mutableStateOf("15") }
        var offerExpiry by remember { mutableStateOf("2026-12-31") }

        AlertDialog(
            onDismissRequest = { showAddOfferDialog = false },
            title = { Text("إضافة عرض ترويجي جديد 🏷️", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offerTitle,
                        onValueChange = { offerTitle = it },
                        label = { Text("عنوان العرض (مثال: خصم الصيف)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offerDesc,
                        onValueChange = { offerDesc = it },
                        label = { Text("تفاصيل العرض والشروط", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offerDiscount,
                        onValueChange = { offerDiscount = it },
                        label = { Text("نسبة الخصم %", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = offerExpiry,
                        onValueChange = { offerExpiry = it },
                        label = { Text("تاريخ الانتهاء (YYYY-MM-DD)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (offerTitle.isNotBlank()) {
                            val newOffer = OfferModel(
                                title = offerTitle,
                                description = offerDesc,
                                discountPercent = offerDiscount.toIntOrNull() ?: 10,
                                validUntil = offerExpiry
                            )
                            val updated = offersList + newOffer
                            saveOffersToFirestore(updated)
                            showAddOfferDialog = false
                            Toast.makeText(context, "✅ تم إضافة العرض وحفظه سحابياً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ ونشر 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOfferDialog = false }) {
                    Text("إلغاء", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        )
    }

    // Add Coupon Dialog
    if (showAddCouponDialog) {
        var couponCode by remember { mutableStateOf("") }
        var discountPct by remember { mutableStateOf("10") }
        var usageLimit by remember { mutableStateOf("50") }

        AlertDialog(
            onDismissRequest = { showAddCouponDialog = false },
            title = { Text("إنشاء كوبون خصم جديد 🎟️", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = couponCode,
                        onValueChange = { couponCode = it.uppercase() },
                        label = { Text("رمز الكوبون (مثال: YEMEN2026)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = discountPct,
                        onValueChange = { discountPct = it },
                        label = { Text("نسبة الخصم %", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usageLimit,
                        onValueChange = { usageLimit = it },
                        label = { Text("حد الاستخدام الأقصى (مرات)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (couponCode.isNotBlank()) {
                            val newCoupon = CouponEntity(
                                id = UUID.randomUUID().toString(),
                                code = couponCode.trim(),
                                discountPercentage = discountPct.toIntOrNull() ?: 10,
                                maxUsageCount = usageLimit.toIntOrNull() ?: 50,
                                usedCount = 0,
                                status = "ACTIVE",
                                expiryTimestamp = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                            )
                            viewModel.saveCoupon(newCoupon)
                            showAddCouponDialog = false
                            Toast.makeText(context, "✅ تم إنشاء الكوبون وحفظه سحابياً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ الكوبون 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCouponDialog = false }) {
                    Text("إلغاء", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        )
    }

    // Bulk Pricing Dialog
    if (showBulkPricingDialog) {
        var percentage by remember { mutableStateOf("10") }
        var isIncrease by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showBulkPricingDialog = false },
            title = { Text("تعديل جماعي للأسعار 📈📉", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تطبيق تعديل نسبوي على جميع المنتجات والخدمات (${myProducts.size}) دفعة واحدة.", fontSize = 11.sp, color = Color.LightGray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isIncrease = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isIncrease) Color(0xFF10B981) else Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("زيادة الأسعار (+)", fontSize = 11.sp, color = if (isIncrease) Color.Black else Color.White)
                        }
                        Button(
                            onClick = { isIncrease = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (!isIncrease) Color(0xFFEF5350) else Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("خفض الأسعار (-)", fontSize = 11.sp, color = if (!isIncrease) Color.White else Color.White)
                        }
                    }
                    OutlinedTextField(
                        value = percentage,
                        onValueChange = { percentage = it },
                        label = { Text("النسبة المئوية %", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pct = percentage.toDoubleOrNull() ?: 10.0
                        myProducts.forEach { prod ->
                            val factor = if (isIncrease) (1.0 + pct / 100.0) else (1.0 - pct / 100.0)
                            val newPrice = (prod.price * factor).coerceAtLeast(100.0)
                            viewModel.saveProduct(prod.copy(price = newPrice))
                        }
                        showBulkPricingDialog = false
                        Toast.makeText(context, "✅ تم تطبيق تعديل الأسعار بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تطبيق الآن ⚡", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkPricingDialog = false }) {
                    Text("إلغاء", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        )
    }
}
