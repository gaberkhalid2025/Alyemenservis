package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ProductEntity
import com.example.utils.VisualThemePalette

/**
 * 📊 ProductGrid Component
 * مكون شبكة/قائمة المنتجات يعرض المنتجات بشكل سلس ومستقل عن MainViewModel
 *
 * @param products قائمة المنتجات
 * @param isOwnerOrAdmin صلاحية المالك/الآدمن للتعديل
 * @param themeColors الألوان المعتمدة للتصميم
 * @param onSaveProduct دالة اختيارية عند تحديث المنتج
 * @param onProductOrderClick دالة الاستدعاء عند اختيار شراء المنتج
 */
@Composable
fun ProductGrid(
    products: List<ProductEntity>,
    isOwnerOrAdmin: Boolean,
    themeColors: VisualThemePalette,
    onSaveProduct: ((ProductEntity) -> Unit)? = null,
    viewModel: Any? = null,
    onProductOrderClick: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        products.forEach { product ->
            ProductListItemCard(
                product = product,
                isOwnerOrAdmin = isOwnerOrAdmin,
                themeColors = themeColors,
                onSaveProduct = onSaveProduct,
                onOrderClick = { onProductOrderClick(product) }
            )
        }
    }
}

