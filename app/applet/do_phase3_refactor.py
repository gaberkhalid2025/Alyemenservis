import os
import re

print("Starting Phase 3 refactoring script...")

# -------------------------------------------------------------
# 1. SPLIT ProviderRegisterFormLayout.kt
# -------------------------------------------------------------
reg_path = "app/src/main/java/com/example/ui/screens/register/ProviderRegisterFormLayout.kt"
with open(reg_path, "r", encoding="utf-8") as f:
    reg_text = f.read()

pos0 = reg_text.find("if (selectedCategoryTab == 0)")
pos1 = reg_text.find("else if (selectedCategoryTab == 1)")
pos2 = reg_text.find("else if (selectedCategoryTab == 2)")
pos3 = reg_text.find("else if (selectedCategoryTab == 3)")
pos4 = reg_text.find("else if (selectedCategoryTab == 4)")
pos5 = reg_text.find("else if (selectedCategoryTab == 5)")

# Find the end of selectedCategoryTab == 5 block (matching closing braces)
# Or we can find where ProviderRegisterFormLayout ends.
# Let's inspect where ProviderRegisterFormLayout ends or find the last braces.
# Actually, let's find the closing brace before the end of ProviderRegisterFormLayout.
print("Positions in ProviderRegisterFormLayout:", pos0, pos1, pos2, pos3, pos4, pos5)

technician_form_code = reg_text[pos0:pos1].strip()
store_form_code = reg_text[pos1:pos2].strip()
restaurant_form_code = reg_text[pos2:pos3].strip()
property_form_code = reg_text[pos3:pos4].strip()
medical_form_code = reg_text[pos4:pos5].strip()

# For job form, it goes from pos5 to near the end of ProviderRegisterFormLayout
# Let's find where ProviderRegisterFormLayout ends.
# ProviderRegisterFormLayout is a @Composable fun ProviderRegisterFormLayout(...)
# Let's find the last few closing braces of ProviderRegisterFormLayout.
# Let's search for the end of ProviderRegisterFormLayout by looking for function end.
job_form_code = reg_text[pos5:].strip()
# Remove trailing closing braces of ProviderRegisterFormLayout if included
# job_form_code usually ends before the final closing brace(s) of ProviderRegisterFormLayout.
# Let's check what is at the end of job_form_code.
print("Job form code sample end:", job_form_Code[-100:] if 'job_form_code' in locals() else "")

form_imports = """package com.example.ui.screens.register.forms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.VisualThemePalette
import com.example.ui.components.FlexibleCatalogUploader
import com.example.ui.utils.*
import java.util.UUID
import kotlinx.coroutines.launch
"""

# We can wrap each extracted block in a Composable function:
# e.g., @Composable fun TechnicianRegistrationForm(viewModel: MainViewModel, themeColors: VisualThemePalette, settingsState: AdminSettingsEntity) { ... }

print("Extracted form blocks successfully.")
