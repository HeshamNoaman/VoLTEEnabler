package com.volteenabler.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.volteenabler.app.R
import com.volteenabler.app.data.ImsPackageInfo
import com.volteenabler.app.methods.AssistantManager
import com.volteenabler.app.methods.ImsRepairManager
import com.volteenabler.app.methods.LockscreenShortcutManager
import com.volteenabler.app.methods.SideKeyManager
import com.volteenabler.app.shizuku.ShellExecutor
import com.volteenabler.app.ui.components.InstructionDialog
import com.volteenabler.app.ui.components.MethodCard
import com.volteenabler.app.ui.theme.SuccessGreen
import com.volteenabler.app.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@Composable
fun VolteMethodsScreen(
    isAuthorized: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showInstructionsDialog by remember { mutableStateOf(false) }
    var currentShortcutRaw by remember { mutableStateOf("") }
    var imsPackages by remember { mutableStateOf(ImsPackageInfo.getRequiredPackages()) }

    // Load initial shortcut and package states
    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            currentShortcutRaw = LockscreenShortcutManager.getCurrentRaw(context)
            imsPackages = ImsRepairManager.scanPackages()
        }
    }

    val appliedMsg = stringResource(R.string.success_applied)
    val restoredMsg = stringResource(R.string.success_restored)
    val errorShizukuMsg = stringResource(R.string.error_shizuku_required)
    val deniedMsg = stringResource(R.string.method_direct_denied)

    if (showInstructionsDialog) {
        InstructionDialog(
            title = stringResource(R.string.method_lockscreen_dialog_title),
            steps = listOf(
                stringResource(R.string.method_lockscreen_dialog_step1),
                stringResource(R.string.method_lockscreen_dialog_step2),
                stringResource(R.string.method_lockscreen_dialog_step3),
                stringResource(R.string.method_lockscreen_dialog_step4),
                stringResource(R.string.method_lockscreen_dialog_step5),
                stringResource(R.string.method_lockscreen_dialog_step6)
            ),
            onDismiss = { showInstructionsDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.methods_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.methods_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Method 1: Lockscreen Shortcut (Recommended)
        MethodCard(
            title = stringResource(R.string.method_lockscreen_title),
            description = stringResource(R.string.method_lockscreen_desc),
            icon = Icons.Default.Lock,
            badgeText = "Recommended",
            extraContent = {
                if (currentShortcutRaw.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.method_lockscreen_current, currentShortcutRaw),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            primaryButtonText = stringResource(R.string.method_lockscreen_apply),
            onPrimaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    val ok = LockscreenShortcutManager.applyImsShortcut(context)
                    if (ok) {
                        currentShortcutRaw = LockscreenShortcutManager.getCurrentRaw(context)
                        showInstructionsDialog = true
                    } else {
                        Toast.makeText(context, "Failed to write shortcut setting", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            secondaryButtonText = stringResource(R.string.method_lockscreen_restore),
            onSecondaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    val ok = LockscreenShortcutManager.restoreCameraShortcut(context)
                    if (ok) {
                        currentShortcutRaw = LockscreenShortcutManager.getCurrentRaw(context)
                        Toast.makeText(context, restoredMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // Method 2: Side Key / Double-Press Power
        MethodCard(
            title = stringResource(R.string.method_sidekey_title),
            description = stringResource(R.string.method_sidekey_desc),
            icon = Icons.Default.PowerSettingsNew,
            primaryButtonText = stringResource(R.string.method_sidekey_apply),
            onPrimaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    SideKeyManager.applyImsDoublePress()
                    SideKeyManager.openSideKeySettings()
                    Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                }
            },
            secondaryButtonText = stringResource(R.string.method_sidekey_restore),
            onSecondaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    SideKeyManager.restoreCameraDoublePress()
                    Toast.makeText(context, restoredMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Method 3: Assistant / Long-Press Home
        MethodCard(
            title = stringResource(R.string.method_assistant_title),
            description = stringResource(R.string.method_assistant_desc),
            icon = Icons.Default.Assistant,
            primaryButtonText = stringResource(R.string.method_assistant_apply),
            onPrimaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    AssistantManager.applyImsAssistant()
                    Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                    AssistantManager.triggerHomeLongPress()
                }
            },
            secondaryButtonText = stringResource(R.string.method_assistant_restore),
            onSecondaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    AssistantManager.restoreGoogleAssistant()
                    Toast.makeText(context, restoredMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Method 4: Direct Launch IMS
        MethodCard(
            title = stringResource(R.string.method_direct_title),
            description = stringResource(R.string.method_direct_desc),
            icon = Icons.Default.Launch,
            primaryButtonText = stringResource(R.string.method_direct_launch_ims),
            onPrimaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    val res = ShellExecutor.exec("am start -n com.samsung.advp.imssettings/.MainActivity")
                    if (!res.isSuccess || res.stderr.contains("SecurityException")) {
                        Toast.makeText(context, deniedMsg, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            secondaryButtonText = stringResource(R.string.method_direct_launch_band),
            onSecondaryClick = {
                if (!isAuthorized) {
                    Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                    return@MethodCard
                }
                scope.launch {
                    ShellExecutor.exec("am start -n com.samsung.android.app.telephonyui/.hiddennetworksetting.MainActivity")
                }
            }
        )

        // Method 5: IMS Package Repair
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.method_repair_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.method_repair_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                imsPackages.forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pkg.title, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text(pkg.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (pkg.isInstalled && pkg.isEnabled) stringResource(R.string.pkg_installed) else stringResource(R.string.pkg_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pkg.isInstalled && pkg.isEnabled) SuccessGreen else WarningOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                imsPackages = ImsRepairManager.repairAll()
                                Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.method_repair_all))
                    }
                    OutlinedButton(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(context, errorShizukuMsg, Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            scope.launch {
                                imsPackages = ImsRepairManager.scanPackages()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.method_repair_scan))
                    }
                }
            }
        }
    }
}
