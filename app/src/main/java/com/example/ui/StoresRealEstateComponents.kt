package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

// =====================================================================
// StoresRealEstateComponents.kt — Shared Core Helpers
// =====================================================================
// Modularized UI components now reside in dedicated specialized files:
//  1. SmartRecommendationsSection.kt        (SmartRecommendationsSection, RecommendationItemCard)
//  2. StoresCatalogComponents.kt            (StoresTabContent, StoreListItemCard)
//  3. PropertiesCatalogComponents.kt        (PropertiesTabContent, PropertyListItemCard)
//  4. StoreDetailsDialog.kt                 (StoreDetailsDialog)
//  5. StoreProductModals.kt                 (BulkPriceAdjusterDialog, QuickAddProductDialog)
//  6. PropertyDetailsDialog.kt              (PropertyDetailsDialog)
//  7. StoreCreateEditDialog.kt              (StoreCreateEditDialog)
//  8. PropertyCreateEditDialog.kt           (PropertyCreateEditDialog)
//  9. LegacyAdminStoresPropertiesPanel.kt   (LegacyAdminStoresPropertiesPanel)
// 10. LegacyAdminJobsAndArchivePanels.kt    (AdminJobsPanel, AdminJobApplicantsPanel,
//                                            AdminCentralizedBlockedPanel, AdminCentralizedDeletedPanel)
// =====================================================================

/**
 * Centralized Yemeni phone validator.
 * Used across StoreCreateEditDialog, PropertyCreateEditDialog, and catalog forms.
 */
internal fun isValidYemeniPhone(phone: String): Boolean {
    val clean = phone.trim().replace(" ", "").replace("+", "")
    return clean.length == 9 &&
        (clean.startsWith("77") || clean.startsWith("73") ||
         clean.startsWith("71") || clean.startsWith("70") ||
         clean.startsWith("78"))
}

/**
 * Shared Base64 ImageBitmap decoder with Compose remember caching.
 */
@Composable
fun rememberBase64Bitmap(base64Str: String): ImageBitmap? {
    return remember(base64Str) {
        if (base64Str.isNotEmpty() && !base64Str.startsWith("http") && !base64Str.startsWith("content")) {
            try {
                val cleanBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
