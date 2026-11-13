package com.example.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.GroupMember
import com.example.utils.GroupStore
import com.example.utils.TrackingShareManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 👥 نافذة تتبع المجموعة (حتى 5 أعضاء، تحديث يدوي فقط — بلا مستمع لحظي لتوفير الإنترنت وحصة Firebase).
 */
@Composable
fun GroupTrackingDialog(
    onMembersUpdated: (List<GroupMember>) -> Unit,
    onFocusMember: (GroupMember) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { GroupStore(context) }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var members by remember { mutableStateOf(store.all()) }
    var nameInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var cooldown by remember { mutableIntStateOf(0) }

    fun refreshAll() {
        if (refreshing || cooldown > 0 || members.isEmpty()) return
        scope.launch {
            refreshing = true
            statusMessage = null
            var updated = members
            members.take(GroupStore.MAX_MEMBERS).forEach { m ->
                val snap = TrackingShareManager.fetch(db, m.token)
                updated = store.updateFromFetch(m.token, snap)
            }
            members = updated
            onMembersUpdated(updated)
            refreshing = false
            cooldown = 15
            while (cooldown > 0) {
                delay(1000L)
                cooldown--
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF00E5FF))
                Text("تتبع المجموعة (${members.size}/${GroupStore.MAX_MEMBERS})", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "تحديث يدوي لتوفير الباقة والبطارية — أضف رمز المشاركة المكون من 24 حرفاً.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("الاسم (مثلاً: أحمد)", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("group_member_name_input")
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    singleLine = true,
                    label = { Text("رمز التتبع أو رابط alyemen://track/...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("group_member_token_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val res = store.add(nameInput, tokenInput)
                            res.onSuccess { list ->
                                members = list
                                nameInput = ""
                                tokenInput = ""
                                statusMessage = "تمت الإضافة — اضغط تحديث الآن لجلب الموقع"
                                onMembersUpdated(list)
                            }.onFailure { err ->
                                statusMessage = err.message ?: "تعذر إضافة العضو"
                            }
                        },
                        enabled = members.size < GroupStore.MAX_MEMBERS,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("group_add_member_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { refreshAll() },
                        enabled = !refreshing && cooldown == 0 && members.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("group_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (refreshing) "جاري..." else if (cooldown > 0) "تحديث ($cooldown)" else "تحديث الآن",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp
                        )
                    }
                }
                statusMessage?.let { Text(it, color = Color(0xFFF59E0B), fontSize = 11.5.sp) }

                if (members.isEmpty()) {
                    Text("لا يوجد أعضاء مضافون بعد.", color = Color(0xFF64748B), fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(members, key = { it.token }) { m ->
                            Surface(
                                onClick = { if (!m.expired && m.lastLat != null && m.lastLng != null) onFocusMember(m) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (m.expired) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(m.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val sub = when {
                                            m.expired -> "انتهت المشاركة"
                                            m.lastLat != null && m.lastLng != null && m.updatedAt > 0L -> {
                                                val mins = ((System.currentTimeMillis() - m.updatedAt) / 60_000L).coerceAtLeast(0L)
                                                "منذ $mins د • اضغط للتركيز"
                                            }
                                            else -> "بانتظار الضغط على «تحديث الآن»"
                                        }
                                        Text(
                                            sub,
                                            color = if (m.expired) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val list = store.remove(m.token)
                                            members = list
                                            onMembersUpdated(list)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف العضو", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
    )
}
