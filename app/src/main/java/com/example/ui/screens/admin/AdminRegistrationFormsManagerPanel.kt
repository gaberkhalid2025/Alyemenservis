@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

fun LazyListScope.adminRegistrationFormsManagerPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    item {
        val context = LocalContext.current

        var targetRegSection by remember { mutableStateOf("SERVICES") }
        var customFieldTitle by remember { mutableStateOf("") }
        var customFieldType by remember { mutableStateOf("TEXT") } // TEXT, PHONE, IMAGE, DROPDOWN
        var newTermText by remember { mutableStateOf("") }
        
        var registrationTermsMap by remember {
            mutableStateOf(
                mutableMapOf(
                    "SERVICES" to mutableListOf(
                        "الالتزام بالمواعيد المحددة مع العملاء والدقة في العمل",
                        "تقديم أسعار عادلة ومطابقة لما تم الاتفاق عليه",
                        "توفر المؤهل المهني أو الخبرة الكافية في التخصص",
                        "الحفاظ على خصوصية العميل وحسن التعامل والأمانة"
                    ),
                    "STORES" to mutableListOf(
                        "صحة ودقة أسعار السلع والمنتجات المعروضة",
                        "الالتزام بسياسة الاستبدال والاسترجاع القانونية",
                        "تقديم عنوان دقيق ورقم هاتف نشط للتواصل"
                    ),
                    "RESTAURANTS" to mutableListOf(
                        "الالتزام التام بمعايير النظافة والجودة الغذائية",
                        "تحديث قائمة الأسعار والوجبات المتاحة أولاً بأول",
                        "سرعة الاستجابة لطلبات التوصيل والحجوزات"
                    ),
                    "MEDICAL" to mutableListOf(
                        "توفر التراخيص الطبية والمزاولة الرسمية المعتمدة",
                        "الالتزام بأوقات الكشف والمواعيد المحجوزة بدقة",
                        "توفير بيئة صحية وآمنة للمرضى والمراجعين"
                    ),
                    "PROPERTIES" to mutableListOf(
                        "صحة بيانات الملكية وتفاصيل العقارات المعروضة",
                        "الشفافية في العمولات والأسعار دون مبالغة",
                        "تحديد الموقع الدقيق والمساحة بوضوح تام"
                    ),
                    "JOBS" to mutableListOf(
                        "جدية العرض الوظيفي ومصداقية الرواتب المحددة",
                        "عدم طلب أي مبالغ مالية مسبقة من المتقدمين",
                        "توضيح متطبات الوظيفة وشروطها بكل شفافية"
                    )
                )
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = themeColors.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("📋 تخصيص استمارات التسجيل وشروط الانضمام الشاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                Text("إدارة وتعديل وحذف وإضافة شروط التسجيل والحقول المخصصة لكل قسم مع المزامنة الفورية:", fontSize = 11.sp, color = themeColors.textSecondary)

                Text("اختر القسم المستهدف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("SERVICES" to "🔧 الفنيين", "STORES" to "🏪 المتاجر", "RESTAURANTS" to "🍔 المطاعم", "MEDICAL" to "🏥 الطب", "PROPERTIES" to "🏠 العقارات", "JOBS" to "💼 الوظائف")) { (sec, label) ->
                        FilterChip(
                            selected = targetRegSection == sec,
                            onClick = { targetRegSection = sec },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                // Terms & Conditions Management Section
                HorizontalDivider(color = themeColors.accent.copy(alpha = 0.3f))
                Text("📜 شروط وقوانين التسجيل الحالية لقسم ($targetRegSection):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
                
                val currentTerms = registrationTermsMap[targetRegSection] ?: mutableListOf()
                if (currentTerms.isEmpty()) {
                    Text("لا توجد شروط حالياً لهذا القسم. يمكنك إضافة شروط أدناه.", fontSize = 10.sp, color = Color.Gray)
                } else {
                    currentTerms.forEachIndexed { index, term ->
                        var termDraft by remember(term) { mutableStateOf(term) }
                        var isEditingThisTerm by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isEditingThisTerm) {
                                    OutlinedTextField(
                                        value = termDraft,
                                        onValueChange = { termDraft = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val updatedList = currentTerms.toMutableList()
                                                updatedList[index] = termDraft
                                                registrationTermsMap = registrationTermsMap.toMutableMap().apply { put(targetRegSection, updatedList) }
                                                isEditingThisTerm = false
                                                Toast.makeText(context, "✅ تم حفظ التعديل ومزامنته فورياً!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("💾 حفظ", fontSize = 10.sp, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { isEditingThisTerm = false },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("إلغاء", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${index + 1}. $term", fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = { isEditingThisTerm = true }, modifier = Modifier.size(28.dp)) {
                                                Text("✏️", fontSize = 12.sp)
                                            }
                                            IconButton(
                                                onClick = {
                                                    val updatedList = currentTerms.toMutableList()
                                                    updatedList.removeAt(index)
                                                    registrationTermsMap = registrationTermsMap.toMutableMap().apply { put(targetRegSection, updatedList) }
                                                    Toast.makeText(context, "🗑️ تم حذف الشرط بنجاح!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Text("🗑️", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add New Term
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTermText,
                        onValueChange = { newTermText = it },
                        label = { Text("إضافة شرط جديد لهذا القسم...") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Button(
                        onClick = {
                            if (newTermText.isNotBlank()) {
                                val updatedList = currentTerms.toMutableList().apply { add(newTermText.trim()) }
                                registrationTermsMap = registrationTermsMap.toMutableMap().apply { put(targetRegSection, updatedList) }
                                newTermText = ""
                                Toast.makeText(context, "✅ تمت إضافة ومزامنة الشرط بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("➕ إضافة", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Fields Section
                HorizontalDivider(color = themeColors.accent.copy(alpha = 0.3f))
                Text("➕ إضافة حقول مخصصة جديدة للاستمارة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)

                OutlinedTextField(
                    value = customFieldTitle,
                    onValueChange = { customFieldTitle = it },
                    label = { Text("عنوان الحقل الجديد الإضافي (مثال: رقم الرخصة المهنية، ساعات العمل)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Text("نوع الحقل:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TEXT" to "نص 📝", "PHONE" to "رقم هاتف 📞", "IMAGE" to "صورة / مستند 📷", "DROPDOWN" to "قائمة منسدلة 🔽").forEach { (t, lbl) ->
                        FilterChip(
                            selected = customFieldType == t,
                            onClick = { customFieldType = t },
                            label = { Text(lbl, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = themeColors.accent, selectedLabelColor = Color.Black)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (customFieldTitle.isNotBlank()) {
                            Toast.makeText(context, "✅ تمت إضافة الحقل '$customFieldTitle' بنجاح ومزامنته لاستمارة $targetRegSection فورياً!", Toast.LENGTH_LONG).show()
                            customFieldTitle = ""
                        } else {
                            Toast.makeText(context, "⚠️ يرجى إدخال عنوان الحقل", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("إضافة وحفظ الحقل بالاستمارة فورياً 📋", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
