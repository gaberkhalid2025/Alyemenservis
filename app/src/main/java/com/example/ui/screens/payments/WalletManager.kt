package com.example.ui.screens.payments

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.VisualThemePalette

data class UserWallet(
    val id: String,
    val type: String,
    val accountNumber: String,
    val ownerName: String
)

@Composable
fun WalletManager(
    themeColors: VisualThemePalette,
    linkedWallets: List<UserWallet> = listOf(
        UserWallet("1", "M-KREEMY", "1234567", "صالح محمد صالح"),
        UserWallet("2", "JEEB", "777888999", "صالح محمد صالح")
    ),
    onWalletAdded: (String, String, String) -> Unit = { _, _, _ -> },
    onWalletDeleted: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("JEEB") }
    var walletNumber by remember { mutableStateOf("") }
    var walletName by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📱 إدارة محافظ الدفع الإلكتروني",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("إضافة ➕", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (linkedWallets.isEmpty()) {
                Text(
                    text = "لم تقم بربط أي محفظة يمنية إلكترونية بعد.",
                    fontSize = 11.sp,
                    color = themeColors.textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                linkedWallets.forEach { wallet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.background, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📱 ${wallet.type}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )
                            Text(
                                text = "رقم الحساب: ${wallet.accountNumber}",
                                fontSize = 11.sp,
                                color = themeColors.textPrimary
                            )
                            Text(
                                text = "اسم الحساب: ${wallet.ownerName}",
                                fontSize = 10.sp,
                                color = themeColors.textSecondary
                            )
                        }

                        IconButton(onClick = { onWalletDeleted(wallet.id) }) {
                            Icon(Icons.Default.Delete, "حذف المحفظة", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("➕ إضافة محفظة أو حساب دفع جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("اختر نوع المحفظة:", fontSize = 11.sp, color = Color.LightGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("JEEB", "KREEMY", "JAWALY").forEach { type ->
                                    val isSel = selectedType == type
                                    Button(
                                        onClick = { selectedType = type },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) themeColors.accent else Color.DarkGray
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(type, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = walletNumber,
                                onValueChange = { walletNumber = it },
                                placeholder = { Text("رقم الحساب أو رقم الهاتف المربوط بالمحفظة", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = walletName,
                                onValueChange = { walletName = it },
                                placeholder = { Text("الاسم الكامل المسجل لدى المحفظة", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (walletNumber.isNotBlank() && walletName.isNotBlank()) {
                                    onWalletAdded(selectedType, walletNumber, walletName)
                                    Toast.makeText(context, "تمت إضافة المحفظة بنجاح!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                    walletNumber = ""
                                    walletName = ""
                                } else {
                                    Toast.makeText(context, "يرجى ملء جميع البيانات المطلوبة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                        ) {
                            Text("تأكيد وحفظ 💾", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء", color = Color.LightGray, fontSize = 11.sp)
                        }
                    },
                    containerColor = themeColors.surface
                )
            }
        }
    }
}
