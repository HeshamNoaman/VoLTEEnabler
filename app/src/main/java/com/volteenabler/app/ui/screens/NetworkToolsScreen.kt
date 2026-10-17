package com.volteenabler.app.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volteenabler.app.R
import com.volteenabler.app.data.ApnProfile
import com.volteenabler.app.shizuku.ShellExecutor
import com.volteenabler.app.ui.components.ApnCard
import kotlinx.coroutines.launch

@Composable
fun NetworkToolsScreen(
    isAuthorized: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val errorShizukuMsg = stringResource(R.string.error_shizuku_required)
    val appliedMsg = stringResource(R.string.success_applied)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.tools_title),
            style = MaterialTheme.typography.titleLarge
        )

        // 1. RadioInfo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tool_radio_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.tool_radio_info_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // 1. Try launching via native Android Intents directly without Shizuku
                        val candidateIntents = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            listOf(
                                Intent().setClassName("com.android.phone", "com.android.phone.settings.RadioInfo"),
                                Intent("android.intent.action.MAIN").setClassName("com.android.phone", "com.android.phone.settings.RadioInfo"),
                                Intent().setClassName("com.android.settings", "com.android.settings.RadioInfo")
                            )
                        } else {
                            listOf(
                                Intent().setClassName("com.android.settings", "com.android.settings.RadioInfo"),
                                Intent().setClassName("com.android.settings", "com.android.settings.TestingSettings"),
                                Intent().setClassName("com.android.phone", "com.android.phone.settings.RadioInfo")
                            )
                        }

                        var launchedDirectly = false
                        for (cand in candidateIntents) {
                            try {
                                cand.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(cand)
                                launchedDirectly = true
                                break
                            } catch (_: Exception) {}
                        }

                        if (!launchedDirectly) {
                            // 2. Fall back to shell command (via Shizuku if available)
                            scope.launch {
                                val commands = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    listOf(
                                        "am start -n com.android.phone/.settings.RadioInfo",
                                        "am start -n com.android.phone/com.android.phone.settings.RadioInfo",
                                        "am start -n com.android.settings/.RadioInfo"
                                    )
                                } else {
                                    listOf(
                                        "am start -n com.android.settings/.RadioInfo",
                                        "am start -n com.android.settings/.TestingSettings",
                                        "am start -n com.android.phone/.settings.RadioInfo"
                                    )
                                }
                                var launchedShell = false
                                for (cmd in commands) {
                                    val res = ShellExecutor.exec(cmd)
                                    if (res.isSuccess &&
                                        !res.stderr.contains("Error type 3") &&
                                        !res.stderr.contains("does not exist")
                                    ) {
                                        launchedShell = true
                                        break
                                    }
                                }
                                if (!launchedShell) {
                                    Toast.makeText(
                                        context,
                                        "RadioInfo not found on this device. Try dialing *#*#4636#*#* manually.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tool_radio_info_btn))
                }
            }
        }

        // 2. APN Settings Shortcut
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SettingsCell,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tool_apn_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.tool_apn_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val subId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            android.telephony.SubscriptionManager.getDefaultDataSubscriptionId().takeIf {
                                it != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
                            } ?: android.telephony.SubscriptionManager.getDefaultSubscriptionId().takeIf {
                                it != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
                            } ?: 1
                        } else {
                            1
                        }

                        val intent = Intent(Settings.ACTION_APN_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("sub_id", subId)
                            putExtra("android.provider.extra.SUB_ID", subId)
                            putExtra("subscription", subId)
                        }

                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            scope.launch {
                                ShellExecutor.exec("am start -a android.settings.APN_SETTINGS --ei sub_id $subId")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tool_apn_open_btn))
                }
            }
        }

        // 3. Show Mobile Data QS Tile
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tool_qs_tile_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.tool_qs_tile_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!isAuthorized) {
                            Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            val currentTiles = ShellExecutor.exec("settings get secure sysui_qs_tiles").stdout
                            val newTiles = if (currentTiles.contains("MobileData") || currentTiles.contains("cell")) {
                                currentTiles
                            } else {
                                "$currentTiles,MobileData,Hotspot"
                            }
                            ShellExecutor.exec("settings put secure sysui_qs_tiles '$newTiles'")
                            Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tool_qs_tile_btn))
                }
            }
        }

        // 4. VoLTE APN Profiles
        Text(
            text = stringResource(R.string.apn_profiles_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        ApnProfile.getDefaultProfiles().forEach { profile ->
            ApnCard(profile = profile)
        }

        // 5. Other OEM Menus (MTK & LG)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.tool_other_oem_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent().apply {
                                    component = android.content.ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                scope.launch {
                                    ShellExecutor.exec("am start -n com.mediatek.engineermode/.EngineerMode")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tool_mtk_engineer))
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent().apply {
                                    component = android.content.ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.bandselect.BandMode")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                scope.launch {
                                    ShellExecutor.exec("am start -n com.mediatek.engineermode/com.mediatek.engineermode.bandselect.BandMode")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tool_mtk_band))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent().apply {
                                component = android.content.ComponentName("com.lge.networksettings", "com.lge.networksettings.MobileNetworkSettingsForGLOBAL")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            scope.launch {
                                ShellExecutor.exec("am start -n com.lge.networksettings/com.lge.networksettings.MobileNetworkSettingsForGLOBAL")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tool_lg_network))
                }
            }
        }

        // 6. Reboot Menu
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.tool_reboot_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch { ShellExecutor.exec("reboot") }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tool_reboot_normal))
                    }
                    Button(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch { ShellExecutor.exec("reboot recovery") }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tool_reboot_recovery))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            scope.launch { ShellExecutor.exec("reboot download") }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tool_reboot_download))
                    }
                    OutlinedButton(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            scope.launch { ShellExecutor.exec("reboot bootloader") }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.tool_reboot_bootloader))
                    }
                }
            }
        }
    }
}
