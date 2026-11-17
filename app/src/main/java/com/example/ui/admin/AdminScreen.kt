package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.SUPER_ADMIN_UID
import com.example.data.model.UserProfile
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.StrawberryDark
import com.example.ui.theme.StrawberryRed
import com.example.ui.theme.StrawberrySoftBg
import com.example.ui.theme.StrawberryTint
import com.example.util.BengaliFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    appConfig: AppConfig,
    users: List<UserProfile>,
    isSaving: Boolean,
    onSaveConfig: (AppConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var appName by remember { mutableStateOf(appConfig.appName) }
    var aratName by remember { mutableStateOf(appConfig.aratName) }
    var commissionText by remember { mutableStateOf(appConfig.defaultCommissionPercent.toString()) }
    var kuliText by remember { mutableStateOf(appConfig.defaultKuliRate.toString()) }
    var welcomeBanner by remember { mutableStateOf(appConfig.welcomeBanner) }
    var grades by remember { mutableStateOf(appConfig.qualityGrades) }
    var newGradeText by remember { mutableStateOf("") }

    LaunchedEffect(appConfig) {
        appName = appConfig.appName
        aratName = appConfig.aratName
        commissionText = appConfig.defaultCommissionPercent.toBigDecimal().stripTrailingZeros().toPlainString()
        kuliText = appConfig.defaultKuliRate.toBigDecimal().stripTrailingZeros().toPlainString()
        welcomeBanner = appConfig.welcomeBanner
        grades = appConfig.qualityGrades
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Admin Header Badge
        item(key = "admin_badge") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StrawberrySoftBg,
                border = BorderStroke(1.dp, StrawberryTint),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.AdminPanelSettings,
                        contentDescription = "সুপার অ্যাডমিন",
                        tint = StrawberryRed,
                        modifier = Modifier.size(30.dp)
                    )
                    Column {
                        Text(
                            text = "সুপার অ্যাডমিন কন্ট্রোল প্যানেল",
                            style = MaterialTheme.typography.titleMedium,
                            color = StrawberryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "এখান থেকে পুরো অ্যাপের সেটিংস রিয়েল-টাইমে নিয়ন্ত্রণ করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Global App Settings Card
        item(key = "admin_settings") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BentoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "অ্যাপ ও আড়ত সেটিংস",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("অ্যাপের নাম (App Name)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = aratName,
                        onValueChange = { aratName = it },
                        label = { Text("ডিফল্ট আড়তের নাম") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = commissionText,
                            onValueChange = { commissionText = it },
                            label = { Text("ডিফল্ট কমিশন (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kuliText,
                            onValueChange = { kuliText = it },
                            label = { Text("ডিফল্ট কুলি খরচ (৳)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = welcomeBanner,
                        onValueChange = { welcomeBanner = it },
                        label = { Text("ওয়েলকাম নোটিশ / ব্যানার টেক্সট") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "ডিফল্ট কোয়ালিটি গ্রেডসমূহ",
                        style = MaterialTheme.typography.labelLarge
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        grades.forEachIndexed { idx, g ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldSoftBg,
                                border = BorderStroke(1.dp, EmeraldTint)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                                ) {
                                    Text(
                                        text = g,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldDark
                                    )
                                    if (grades.size > 1) {
                                        IconButton(
                                            onClick = {
                                                grades = grades.toMutableList().apply { removeAt(idx) }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "সরিয়ে ফেলুন",
                                                tint = StrawberryRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newGradeText,
                            onValueChange = { newGradeText = it },
                            label = { Text("নতুন গ্রেড যোগ করুন") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = newGradeText.trim()
                                if (trimmed.isNotEmpty() && !grades.contains(trimmed)) {
                                    grades = grades + trimmed
                                    newGradeText = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StrawberryRed),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "যোগ করুন")
                        }
                    }

                    Button(
                        onClick = {
                            val comm = BengaliFormatter.parseNumericInput(commissionText) ?: 7.0
                            val kuli = BengaliFormatter.parseNumericInput(kuliText) ?: 0.0
                            onSaveConfig(
                                AppConfig(
                                    appName = appName,
                                    aratName = aratName,
                                    defaultCommissionPercent = comm,
                                    defaultKuliRate = kuli,
                                    welcomeBanner = welcomeBanner,
                                    qualityGrades = grades
                                )
                            )
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_admin_config_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("লাইভ সেটিংস আপডেট করুন")
                    }
                }
            }
        }

        // Registered Users Section
        item(key = "users_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Rounded.People, contentDescription = null, tint = EmeraldGreen)
                Text(
                    text = "নিবন্ধিত ব্যবহারকারী (${BengaliFormatter.formatInt(users.size)} জন)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(items = users, key = { it.userId.ifBlank { it.id } }) { user ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BentoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = user.displayName.ifBlank { "নামহীন ব্যবহারকারী" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (user.aratName.isNotBlank()) {
                            Text(
                                text = "আড়ত: ${user.aratName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldDark
                            )
                        }
                    }

                    if (user.userId == SUPER_ADMIN_UID) {
                        Surface(shape = RoundedCornerShape(8.dp), color = StrawberryTint) {
                            Text(
                                text = "সুপার অ্যাডমিন",
                                style = MaterialTheme.typography.labelSmall,
                                color = StrawberryDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
