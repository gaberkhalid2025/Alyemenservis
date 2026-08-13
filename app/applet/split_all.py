import os, re

dir_path = "app/src/main/java/com/example/ui/screens/admin"

with open(os.path.join(dir_path, "AdminPanelLayout.kt")) as f:
    full_text = f.read()

lines = full_text.split("\n")

state_vars = []
for idx in range(120, 280):
    l = lines[idx]
    if "var " in l and "by remember" in l:
        match = re.search(r"var\s+([a-zA-Z0-9_]+)\s+by\s+(remember.*)", l)
        if match:
            vname = match.group(1)
            rem_expr = match.group(2)
            state_vars.append((vname, rem_expr))

def infer_type(vname, expr):
    if "mutableStateOf<" in expr:
        match = re.search(r"mutableStateOf<([^>]+)>", expr)
        if match:
            t = match.group(1).strip()
            if "com.example.data." in t:
                t = t.replace("com.example.data.", "")
            return t
    if vname in ["isAuthorized", "isNewRequirementMandatory", "editShowVipBadge", "editShowVerifiedBadge", "editShowRecommendedBadge", "editShowCallButton", "editShowWhatsappButton", "editShowDetailsButton", "editShowBookButton", "editShowLoyaltyBanner", "showWipeConfirmDialog", "wipeProvidersChecked", "wipeBookingsChecked", "wipeChatsChecked", "wipeNotifsChecked", "wipeReportsChecked", "wipeCategoriesChecked", "wipePendingChecked", "wipeBannersChecked", "wipeSupervisorsChecked", "wipeCitiesChecked", "wipeThemesChecked", "manualIsVipGolden", "showExportReportPasswordDialog"]:
        return "Boolean"
    if vname in ["activeSubTab", "inputPasscode", "adminReqSubTab", "adminBookingSubTab", "adminChatSubTab", "adminAddSubTab", "adminReviewSubTab", "adminNotifSubTab", "adminVipSubTab", "adminBannerSubTab", "adminPasswordSubTab", "providerRejectionReasonText", "editCatName", "editCatIcon", "newCatName", "newCatIcon", "bookingRejectionReasonInput", "adminChatReplyInput", "backupJsonStringState", "restoreJsonInputState", "notifTitleInput", "notifMsgInput", "notifTargetType", "notifTargetValue", "notifDelayHours", "notifValidityHours", "editPrimaryHex", "editSecondaryHex", "editCardBgHex", "editProviderNameHex", "editLocationHex", "editRatingHex", "editVipBadgeHex", "editVerifiedHex", "editRecommendedHex", "editFontSelected", "requirementItemInput", "categoryManagementMode", "editCallButtonColorHex", "editWhatsappButtonColorHex", "editDetailsButtonColorHex", "editBookButtonColorHex", "wipeInputPassword", "manualName", "manualPhone", "manualCategoryId", "manualStreet", "manualCityId", "manualPhotoUrl", "manualIdCardUrl", "manualForensicUrl", "manualPriceValue", "newCityArName", "newCityEnName", "newCityIcon", "complaintsSearchQuery", "activeProvidersSearchQuery", "activeJobsSearchQuery", "storesSearchQuery", "restaurantsSearchQuery", "medicalSearchQuery", "propertiesSearchQuery", "applicantsSearchQuery", "editProviderPhone", "editProviderCategoryId", "supervisorInputName", "supervisorInputRole", "supervisorInputPasscode", "exportReportPasswordInput"]:
        return "String"
    if vname in ["editChatIconSize", "editChatIconX", "editChatIconY", "editAssistantIconSize", "editAssistantIconX", "editAssistantIconY", "editCoverHeight", "editAvatarSize", "editElementSpacing", "editCardPadding", "editMaxWorkPhotos", "elementSpacingPadding", "containerCardPadding"]:
        return "Float"
    if vname == "requirementsListState":
        return "List<String>"
    if "true" in expr or "false" in expr:
        return "Boolean"
    if "0f" in expr or "12f" in expr or "14f" in expr or "toFloat()" in expr:
        return "Float"
    if "emptyList()" in expr or "split(" in expr:
        return "List<String>"
    return "String"

typed_vars = [(vname, rem_expr, infer_type(vname, rem_expr)) for vname, rem_expr in state_vars]

# Generate AdminMainPanel.kt
header = """@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.utils.*
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.VisualThemePalette
import com.example.viewmodels.*
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
"""

state_params = []
state_delegates = []

for vname, rem_expr, vtype in typed_vars:
    state_params.append("    val " + vname + "State: MutableState<" + vtype + ">")
    state_delegates.append("    var " + vname + ": " + vtype + "\n        get() = " + vname + "State.value\n        set(v) { " + vname + "State.value = v }")

sep_comma = ",\n"
sep_nl = "\n"

params_str = sep_comma.join(state_params)
delegates_str = sep_nl.join(state_delegates)
main_panel_code = header + "\n\nclass AdminPanelState(\n" + params_str + "\n) {\n" + delegates_str + "\n}\n"

with open(os.path.join(dir_path, "AdminMainPanel.kt"), "w") as f:
    f.write(main_panel_code)

print("Generated AdminMainPanel.kt")

# Refactor AdminPanelLayout.kt
new_state_decls = []
state_inst_args = []

for vname, rem_expr, vtype in typed_vars:
    fixed_rem_expr = rem_expr
    if "mutableStateOf" not in rem_expr:
        clean_expr = rem_expr.replace("remember", "").strip()
        fixed_rem_expr = "remember { mutableStateOf<" + vtype + ">(" + clean_expr + ") }"
    elif "mutableStateOf<" + vtype + ">" not in rem_expr and "mutableStateOf(" in rem_expr:
        fixed_rem_expr = rem_expr.replace("mutableStateOf(", "mutableStateOf<" + vtype + ">(")
    
    new_state_decls.append("    val " + vname + "State = " + fixed_rem_expr + "\n    var " + vname + " by " + vname + "State")
    state_inst_args.append("        " + vname + "State = " + vname + "State")

state_decls_code = sep_nl.join(new_state_decls)
args_str = sep_comma.join(state_inst_args)
inst_code = "    val adminPanelState = remember {\n        AdminPanelState(\n" + args_str + "\n        )\n    }"

top_part = sep_nl.join(lines[:120]) + "\n" + state_decls_code + "\n\n" + inst_code + "\n"
middle_part = sep_nl.join(lines[280:555])

panel_calls = """
            adminRequestsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminProvidersPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminBookingsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminNotificationsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminChatPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminBannersPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminCategoriesPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminPaymentsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminSettingsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminBackupPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)
            adminSupervisorsPanel(viewModel = viewModel, themeColors = themeColors, state = adminPanelState)

            if (activeSubTab == "BACKDOOR" && adminRole == "OWNER") {
                item {
                    OwnerBackdoorPanelLayout(viewModel = viewModel, themeColors = themeColors)
                }
            }
"""

dialogs_start_idx = -1
for idx, l in enumerate(lines):
    if "// ------------------ POPUP CONFIRMATION CONTEXT DIALOGS ------------------" in l:
        dialogs_start_idx = idx
        break

bottom_part = sep_nl.join(lines[dialogs_start_idx:])

final_layout = top_part + middle_part + "\n" + panel_calls + "\n" + bottom_part

with open(os.path.join(dir_path, "AdminPanelLayout.kt"), "w") as f:
    f.write(final_layout)

print("Updated AdminPanelLayout.kt successfully")
