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
    var showSideKeyDialog by remember { mutableStateOf(false) }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var currentShortcutRaw by remember { mutableStateOf("") }
    var imsPackages by remember { mutableStateOf(ImsPackageInfo.getRequiredPackages()) }

    // Load initial shortcut and package states immediately
    LaunchedEffect(Unit, isAuthorized) {
        currentShortcutRaw = LockscreenShortcutManager.getCurrentRaw(context)
        imsPackages = ImsRepairManager.scanPackages(context)
    }

    val appliedMsg = stringResource(R.string.success_applied)
    val restoredMsg = stringResource(R.string.success_restored)
    val errorShizukuMsg = stringResource(R.string.error_shizuku_required)

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

    if (showSideKeyDialog) {
        InstructionDialog(
            title = stringResource(R.string.method_sidekey_dialog_title),
            steps = listOf(
                stringResource(R.string.method_sidekey_dialog_step1),
                stringResource(R.string.method_sidekey_dialog_step2),
                stringResource(R.string.method_sidekey_dialog_step3),
                stringResource(R.string.method_sidekey_dialog_step4)
            ),
            actionButtonText = stringResource(R.string.method_sidekey_dialog_action),
            onActionClick = {
                showSideKeyDialog = false
                showInstructionsDialog = true
            },
            onDismiss = { showSideKeyDialog = false }
        )
    }

    if (showAssistantDialog) {
        InstructionDialog(
            title = stringResource(R.string.method_assistant_dialog_title),
            steps = listOf(
                stringResource(R.string.method_assistant_dialog_step1),
                stringResource(R.string.method_assistant_dialog_step2),
                stringResource(R.string.method_assistant_dialog_step3),
                stringResource(R.string.method_assistant_dialog_step4),
                stringResource(R.string.method_assistant_dialog_step5),
                stringResource(R.string.method_assistant_dialog_step6)
            ),
            actionButtonText = stringResource(R.string.method_assistant_dialog_action),
            onActionClick = {
                showAssistantDialog = false
            },
            onDismiss = { showAssistantDialog = false }
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

        // Method 1: Lockscreen Shortcut (Recommended - Standalone / No Shizuku needed)
        MethodCard(
            title = stringResource(R.string.method_lockscreen_title),
            description = stringResource(R.string.method_lockscreen_desc),
            icon = Icons.Default.Lock,
            badgeText = stringResource(R.string.badge_recommended),
            extraContent = {
                if (currentShortcutRaw.isNotBlank()) {
                    val formatted = remember(currentShortcutRaw) {
                        LockscreenShortcutManager.getFormattedShortcuts(context, currentShortcutRaw)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.method_lockscreen_left, formatted.leftLabel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.method_lockscreen_right, formatted.rightLabel),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (formatted.isRightIms) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                        color = if (formatted.isRightIms) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (formatted.isRightIms) {
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.2f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "IMS ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            primaryButtonText = stringResource(R.string.method_lockscreen_apply),
            onPrimaryClick = {
                scope.launch {
                    val canWrite = LockscreenShortcutManager.canWriteSettings(context)
                    if (!isAuthorized && !canWrite) {
                        Toast.makeText(context, "Please allow 'Modify system settings' or connect Shizuku", Toast.LENGTH_LONG).show()
                        LockscreenShortcutManager.requestWriteSettingsPermission(context)
                        return@launch
                    }
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
                scope.launch {
                    val canWrite = LockscreenShortcutManager.canWriteSettings(context)
                    if (!isAuthorized && !canWrite) {
                        Toast.makeText(context, "Please allow 'Modify system settings' or connect Shizuku", Toast.LENGTH_LONG).show()
                        LockscreenShortcutManager.requestWriteSettingsPermission(context)
                        return@launch
                    }
                    val ok = LockscreenShortcutManager.restoreCameraShortcut(context)
                    if (ok) {
                        currentShortcutRaw = LockscreenShortcutManager.getCurrentRaw(context)
                        Toast.makeText(context, restoredMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // Method 2: Side Key / Double-Press Power (Android 10-12 only; blocked on Android 13/14)
        val isAndroid13Plus = android.os.Build.VERSION.SDK_INT >= 33

        MethodCard(
            title = stringResource(R.string.method_sidekey_title),
            description = stringResource(R.string.method_sidekey_desc),
            icon = Icons.Default.PowerSettingsNew,
            extraContent = {
                if (isAndroid13Plus) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.method_sidekey_blocked_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isAuthorized) {
                                    showSideKeyDialog = true
                                    return@OutlinedButton
                                }
                                scope.launch {
                                    SideKeyManager.triggerDoublePower()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.method_sidekey_trigger),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            },
            primaryButtonText = if (isAndroid13Plus) {
                stringResource(R.string.method_sidekey_why_blocked)
            } else {
                stringResource(R.string.method_sidekey_apply)
            },
            onPrimaryClick = {
                if (isAndroid13Plus || !isAuthorized) {
                    showSideKeyDialog = true
                    return@MethodCard
                }
                scope.launch {
                    val ok = SideKeyManager.applyImsDoublePress()
                    if (ok) {
                        Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                        kotlinx.coroutines.delay(400)
                        SideKeyManager.triggerDoublePower()
                    } else {
                        showSideKeyDialog = true
                    }
                }
            },
            secondaryButtonText = stringResource(R.string.method_sidekey_restore),
            onSecondaryClick = {
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
            extraContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isAuthorized) {
                                showAssistantDialog = true
                                return@OutlinedButton
                            }
                            scope.launch {
                                AssistantManager.triggerHomeLongPress()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.method_assistant_trigger),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            // Opens Assistant Settings screen natively without Shizuku!
                            AssistantManager.openAssistantSettings(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.method_assistant_open_settings),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            primaryButtonText = if (isAuthorized) {
                stringResource(R.string.method_assistant_apply)
            } else {
                stringResource(R.string.method_assistant_open_settings)
            },
            onPrimaryClick = {
                if (!isAuthorized) {
                    showAssistantDialog = true
                    return@MethodCard
                }
                scope.launch {
                    val ok = AssistantManager.applyImsAssistant()
                    if (ok) {
                        Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                        AssistantManager.triggerHomeLongPress()
                    } else {
                        showAssistantDialog = true
                    }
                }
            },
            secondaryButtonText = stringResource(R.string.method_assistant_restore),
            onSecondaryClick = {
                if (!isAuthorized) {
                    AssistantManager.openAssistantSettings(context)
                    return@MethodCard
                }
                scope.launch {
                    AssistantManager.restoreGoogleAssistant()
                    Toast.makeText(context, restoredMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
 
        // Method 4: IMS Package Repair
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
                                imsPackages = ImsRepairManager.repairAll(context)
                                Toast.makeText(context, appliedMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.method_repair_all))
                    }
                    OutlinedButton(
                        onClick = {
                            // Package scan works natively without Shizuku
                            scope.launch {
                                imsPackages = ImsRepairManager.scanPackages(context)
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
