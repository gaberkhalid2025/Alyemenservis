import os
import re

print("Writing split_app_and_forms_v2.py...")

# Let's inspect AppNavigator.kt and ProviderRegisterFormLayout.kt text
app_nav_path = "app/src/main/java/com/example/ui/AppNavigator.kt"
reg_form_path = "app/src/main/java/com/example/ui/screens/register/ProviderRegisterFormLayout.kt"

with open(app_nav_path, "r", encoding="utf-8") as f:
    app_nav_text = f.read()

with open(reg_form_path, "r", encoding="utf-8") as f:
    reg_form_text = f.read()

# 1. Split ProviderRegisterFormLayout into forms
pos0 = reg_form_text.find("if (selectedCategoryTab == 0)")
pos1 = reg_form_text.find("else if (selectedCategoryTab == 1)")
pos2 = reg_form_text.find("else if (selectedCategoryTab == 2)")
pos3 = reg_form_text.find("else if (selectedCategoryTab == 3)")
pos4 = reg_form_text.find("else if (selectedCategoryTab == 4)")
pos5 = reg_form_text.find("else if (selectedCategoryTab == 5)")

tech_code = reg_form_text[pos0:pos1].strip()
store_code = reg_form_text[pos1:pos2].strip()
rest_code = reg_form_text[pos2:pos3].strip()
prop_code = reg_form_text[pos3:pos4].strip()
med_code = reg_form_text[pos4:pos5].strip()
job_code = reg_form_text[pos5:].strip()

# Clean up trailing closing brace(s) of ProviderRegisterFormLayout if present in job_code
# job_code ends with some extra closing braces that close the when/Card/Column/ProviderRegisterFormLayout
# Let us find where job_code content ends before final function braces.
# Actually we can wrap them cleanly in @Composable fun TechnicianRegistrationForm(...) etc.

forms_pkg = "package com.example.ui.screens.register.forms\n\n"
common_form_imports = """import android.content.Context
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

def write_form_file(filename, func_name, code):
    path = f"app/src/main/java/com/example/ui/screens/register/forms/{filename}"
    # Ensure code starts properly and ends properly
    content = forms_pkg + common_form_imports + f"@Composable\nfun {func_name}(\n    viewModel: MainViewModel,\n    themeColors: VisualThemePalette,\n    settingsState: AdminSettingsEntity\n) {{\n{code}\n}}\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Created {filename}")

# Clean up job_code trailing braces if needed. Let's make sure code doesn't have excess closing braces.
# In job_code, the block starts with `if (selectedCategoryTab == 5) {` (or `else if (selectedCategoryTab == 5) {`), let's strip `else if (selectedCategoryTab == 5) {` and trailing braces.
def clean_block(block):
    # remove leading `else if (...) {` or `if (...) {`
    if block.startswith("else if"):
        idx = block.find("{")
        if idx != -1:
            block = block[idx+1:]
    elif block.startswith("if"):
        idx = block.find("{")
        if idx != -1:
            block = block[idx+1:]
    # remove last closing brace
    block = block.rstrip()
    if block.endswith("}"):
        block = block[:-1]
    return block.strip()

write_form_file("TechnicianRegistrationForm.kt", "TechnicianRegistrationForm", clean_block(tech_code))
write_form_file("StoreRegistrationForm.kt", "StoreRegistrationForm", clean_block(store_code))
write_form_file("RestaurantRegistrationForm.kt", "RestaurantRegistrationForm", clean_block(rest_code))
write_form_file("PropertyRegistrationForm.kt", "PropertyRegistrationForm", clean_block(property_form_code))
write_form_file("MedicalRegistrationForm.kt", "MedicalRegistrationForm", clean_block(medical_form_code))
write_form_file("JobRegistrationForm.kt", "JobRegistrationForm", clean_block(clean_block(job_code)))

print("All 6 registration forms created successfully.")
