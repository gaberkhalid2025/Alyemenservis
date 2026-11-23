package com.example.ui.screens.register.forms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.submitJoinForm
import com.example.ui.setJoinRequestPhone
import com.example.utils.VisualThemePalette

/**
 * 👨‍💼 JobSeekerForm
 * استمارة المتقدم للوظائف (باحث عن عمل)
 * الحقول المعتمدة: الاسم الثلاثي، الهاتف، كلمة المرور، التأكيد، المجال المطلوب، سنوات الخبرة، المدينة، الحي، المؤهل العلمي، نبذة، رابط السيرة الذاتية
 */
@Composable
fun JobSeekerForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    UnifiedRegistrationForm(
        role = "JOB_SEEKER",
        themeColors = themeColors,
        onRegistrationSuccess = { data ->
            val fullName = (data["fullName"] as? String) ?: ""
            val phone = (data["phone"] as? String) ?: ""
            val password = (data["password"] as? String) ?: ""
            val city = (data["city"] as? String) ?: "صنعاء"
            val address = (data["address"] as? String) ?: ""
            val craftType = (data["craftType"] as? String)?.takeIf { it.isNotBlank() } ?: "باحث عن عمل"
            val resolvedGps = (data["gpsCoords"] as? String)?.takeIf { it.isNotBlank() } ?: "15.36940,44.19100"
            val qualification = (data["cvUrlOrQualification"] as? String) ?: ""
            val experience = (data["experienceYears"] as? String) ?: ""
            val notes = (data["description"] as? String) ?: ""

            val cleanPhone = phone.trim().replace(" ", "").replace("+", "")

            val metadataJson = buildString {
                append("{")
                append("\"qualification\":\"").append(qualification.replace("\"", "\\\"")).append("\",")
                append("\"experienceYears\":\"").append(experience.replace("\"", "\\\"")).append("\",")
                append("\"description\":\"").append(notes.replace("\"", "\\\"")).append("\"")
                append("}")
            }

            viewModel.submitJoinForm(
                context = context,
                name = fullName,
                phone = cleanPhone,
                catId = "JOB_SEEKER",
                area = city,
                neighborhood = address,
                photoPath = (data["imageUri"] as? String) ?: "",
                idCardPath = (data["idCardUri"] as? String) ?: "",
                gpsCoords = resolvedGps,
                customCategoryName = craftType,
                password = password,
                productAttachmentsJson = metadataJson,
                onSubmissionResult = { success ->
                    if (success) {
                        viewModel.setJoinRequestPhone(context, cleanPhone)
                        viewModel.triggerNotification("✅ تم إرسال طلب التقديم للوظيفة بنجاح، جاري المراجعة من الإدارة")
                        onSuccess()
                    }
                }
            )
        }
    )
}
