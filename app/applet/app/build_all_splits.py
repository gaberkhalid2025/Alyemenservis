import os
import re

print("Starting build_all_splits.py...")

reg_path = "app/src/main/java/com/example/ui/screens/register/ProviderRegisterFormLayout.kt"
with open(reg_path, "r", encoding="utf-8") as f:
    reg_text = f.read()

pos0 = reg_text.find("if (selectedCategoryTab == 0)")
pos1 = reg_text.find("else if (selectedCategoryTab == 1)")
pos2 = reg_text.find("else if (selectedCategoryTab == 2)")
pos3 = reg_text.find("else if (selectedCategoryTab == 3)")
pos4 = reg_text.find("else if (selectedCategoryTab == 4)")
pos5 = reg_text.find("else if (selectedCategoryTab == 5)")

tech_code = reg_text[pos0:pos1].strip()
store_code = reg_text[pos1:pos2].strip()
rest_code = reg_text[pos2:pos3].strip()
prop_code = reg_text[pos3:pos4].strip()
med_code = reg_text[pos4:pos5].strip()
job_code = reg_text[pos5:].strip()

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

def clean_block(block):
    if block.startswith("else if"):
        idx = block.find("{")
        if idx != -1:
            block = block[idx+1:]
    elif block.startswith("if"):
        idx = block.find("{")
        if idx != -1:
            block = block[idx+1:]
    block = block.rstrip()
    if block.endswith("}"):
        block = block[:-1]
    return block.strip()

os.makedirs("app/src/main/java/com/example/ui/screens/register/forms", exist_ok=True)
os.makedirs("app/src/main/java/com/example/ui/navigation", exist_ok=True)

def write_form(filename, func_name, code):
    path = f"app/src/main/java/com/example/ui/screens/register/forms/{filename}"
    content = forms_pkg + common_form_imports + f"@Composable\nfun {func_name}(\n    viewModel: MainViewModel,\n    themeColors: VisualThemePalette,\n    settingsState: AdminSettingsEntity\n) {{\n{code}\n}}\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Wrote {filename}")

write_form("TechnicianRegistrationForm.kt", "TechnicianRegistrationForm", clean_block(tech_code))
write_form("StoreRegistrationForm.kt", "StoreRegistrationForm", clean_block(store_code))
write_form("RestaurantRegistrationForm.kt", "RestaurantRegistrationForm", clean_block(rest_code))
write_form("PropertyRegistrationForm.kt", "PropertyRegistrationForm", clean_block(prop_code))
write_form("MedicalRegistrationForm.kt", "MedicalRegistrationForm", clean_block(med_code))
write_form("JobRegistrationForm.kt", "JobRegistrationForm", clean_block(clean_block(job_code)))

print("Form splitting completed successfully.")
