package com.example.ui.screens.admin.sections

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminCategoriesSection(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("⚡") }

    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editIcon by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "➕ إضافة قسم أو تصنيف جديد",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("اسم القسم", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent
                        )
                    )

                    OutlinedTextField(
                        value = newCategoryIcon,
                        onValueChange = { newCategoryIcon = it },
                        label = { Text("الأيقونة/إيموجي", color = Color.Gray) },
                        modifier = Modifier.width(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = themeColors.accent
                        )
                    )
                }

                Button(
                    onClick = {
                        if (newCategoryName.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم القسم", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addNewCategory(newCategoryName, newCategoryName, newCategoryIcon, "")
                            Toast.makeText(context, "تمت إضافة القسم بنجاح", Toast.LENGTH_SHORT).show()
                            newCategoryName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة القسم", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "🗂️ الأقسام والتصنيفات المسجلة (${categories.size})",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            items(categories, key = { it.id }) { cat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = cat.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }

                        Row {
                            IconButton(onClick = {
                                editingCategory = cat
                                editName = cat.name
                                editIcon = cat.icon
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = themeColors.accent)
                            }
                            IconButton(onClick = {
                                viewModel.deleteCategory(cat.id)
                                Toast.makeText(context, "تم حذف القسم", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350))
                            }
                        }
                    }
                }
            }
        }
    }

    editingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("تعديل بيانات القسم", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم القسم الجديد", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = editIcon,
                        onValueChange = { editIcon = it },
                        label = { Text("أيقونة القسم", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewCategory(editName, editName, editIcon, "")
                        Toast.makeText(context, "تم تحديث بيانات القسم بنجاح", Toast.LENGTH_SHORT).show()
                        editingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ التغيرات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
