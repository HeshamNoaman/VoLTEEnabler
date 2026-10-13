package com.volteenabler.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volteenabler.app.shizuku.ShellExecutor
import kotlinx.coroutines.launch

// ─── Brand colours matching Force LTE Only (4G/5G) app ───────────────────────
private val ForceLteBg        = Color(0xFF0A3240)   // deep teal-navy background
private val ForceLteCard      = Color(0xFF0D4A5E)   // button/card fill
private val ForceLteAccent    = Color(0xFF1A7A8A)   // lighter teal accent
private val ForceLteText      = Color(0xFFE0F7FA)   // near-white teal text
private val ForceLteSubText   = Color(0xFF80DEEA)   // muted teal
private val ForceLteOutlined  = Color(0xFF26C6DA)   // outline button stroke

// ─── 3 RadioInfo launch methods (matching the reference screenshot) ──────────
private data class RadioMethod(
    val label: String,
    val description: String,
    // adb shell commands to try in order (fallback chain)
    val commands: List<String>
)

private val RADIO_METHODS = listOf(
    RadioMethod(
        label = "METHOD1: (ANDROID BELOW 11)",
        description = "Direct activity launch via com.android.settings — works on Android 6–10.",
        commands = listOf(
            "am start -n com.android.settings/.RadioInfo",
            "am start -n com.android.settings/.TestingSettings"
        )
    ),
    RadioMethod(
        label = "METHOD2: (ANDROID 11+)",
        description = "Phone-app activity path used on Android 11, 12, 13 and 14 devices.",
        commands = listOf(
            "am start -n com.android.phone/.settings.RadioInfo",
            "am start -n com.android.phone/com.android.phone.settings.RadioInfo",
            "am start -n com.android.phone/.TestingMenu"
        )
    ),
    RadioMethod(
        label = "METHOD3: (LEGACY)",
        description = "USSD / tel-URI trick — dials *#*#4636#*#* to open Phone Info on older ROMs.",
        commands = listOf(
            "am start -a android.intent.action.CALL -d tel:*%23*%234636%23*%23*",
            "am start -n com.android.phone/.MMIDialogActivity -d tel:*%23*%234636%23*%23*"
        )
    )
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun RadioInfoScreen(
    isAuthorized: Boolean,
    modifier: Modifier = Modifier
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    var showHow  by remember { mutableStateOf(false) }
    var showAdv  by remember { mutableStateOf(false) }

    if (showHow) {
        HowToUseDialog(onDismiss = { showHow = false })
    }
    if (showAdv) {
        AdvancedRadioDialog(
            isAuthorized = isAuthorized,
            onDismiss    = { showAdv = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ForceLteBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Big "4G" hero ─────────────────────────────────────────────
            Text(
                text       = "4G",
                fontSize   = 96.sp,
                fontWeight = FontWeight.Bold,
                color      = ForceLteAccent,
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 3 Method buttons ──────────────────────────────────────────
            RADIO_METHODS.forEach { method ->
                ForceLteButton(
                    label       = method.label,
                    description = method.description,
                    onClick     = {
                        if (!isAuthorized) {
                            Toast.makeText(
                                context,
                                "Shizuku permission required for this action.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@ForceLteButton
                        }
                        scope.launch {
                            var success = false
                            for (cmd in method.commands) {
                                val res = ShellExecutor.exec(cmd)
                                if (res.isSuccess && !res.stderr.contains("Error")) {
                                    success = true
                                    break
                                }
                            }
                            if (!success) {
                                Toast.makeText(
                                    context,
                                    "Could not open RadioInfo. Try another method.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }

            // ── Advanced Settings ─────────────────────────────────────────
            ForceLteButton(
                label       = "ADVANCED SETTINGS",
                description = "Samsung IMS, Band Selection, MTK Engineer Mode & more.",
                onClick     = { showAdv = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── How to Use (outlined / ghost button) ──────────────────────
            OutlinedButton(
                onClick = { showHow = true },
                shape   = RoundedCornerShape(50),
                border  = androidx.compose.foundation.BorderStroke(1.5.dp, ForceLteOutlined),
                colors  = ButtonDefaults.outlinedButtonColors(
                    contentColor  = ForceLteOutlined,
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .widthIn(min = 180.dp)
                    .height(44.dp)
            ) {
                Text(
                    text       = "HOW TO USE ?",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize   = 13.sp
                )
            }
        }
    }
}

// ─── Reusable filled teal button ─────────────────────────────────────────────

@Composable
private fun ForceLteButton(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        targetValue    = if (pressed) ForceLteAccent else ForceLteCard,
        animationSpec  = tween(120),
        label          = "btn_bg"
    )

    Button(
        onClick  = {
            pressed = true
            onClick()
        },
        shape    = RoundedCornerShape(50),
        colors   = ButtonDefaults.buttonColors(
            containerColor = ForceLteCard,
            contentColor   = ForceLteText
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation  = 4.dp,
            pressedElevation  = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text          = label,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize      = 13.sp,
            textAlign     = TextAlign.Center,
            color         = ForceLteText
        )
    }
}

// ─── "How to Use" dialog ─────────────────────────────────────────────────────

@Composable
private fun HowToUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = ForceLteBg,
        titleContentColor = ForceLteText,
        textContentColor  = ForceLteSubText,
        title = {
            Text("How to Open RadioInfo", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "1. Make sure Shizuku is installed and running (check Home screen).",
                    "2. Grant Shizuku permission to this app when prompted.",
                    "3. Tap METHOD1 first — it works on most Android 6–10 devices.",
                    "4. If METHOD1 fails, try METHOD2 (Android 11+).",
                    "5. Use METHOD3 (USSD) as a last-resort on very old or custom ROMs.",
                    "6. Inside RadioInfo → set \"Preferred Network Type\" to LTE/NR.",
                    "7. Optionally tick \"VoLTE Provisioned\" if your carrier supports it."
                ).forEach { step ->
                    Text(
                        text     = step,
                        fontSize = 14.sp,
                        color    = ForceLteSubText
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("GOT IT", color = ForceLteOutlined, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ─── "Advanced Settings" dialog ──────────────────────────────────────────────

@Composable
private fun AdvancedRadioDialog(
    isAuthorized: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    data class AdvAction(val label: String, val cmd: String)
    val actions = listOf(
        AdvAction("Samsung IMS Settings",        "am start -n com.samsung.advp.imssettings/.MainActivity"),
        AdvAction("Samsung Band Selection",      "am start -n com.samsung.android.app.telephonyui/.hiddennetworksetting.MainActivity"),
        AdvAction("MTK Engineer Mode",           "am start -n com.mediatek.engineermode/.EngineerMode"),
        AdvAction("MTK Band Select",             "am start -n com.mediatek.engineermode/com.mediatek.engineermode.bandselect.BandMode"),
        AdvAction("LG Mobile Network Settings", "am start -n com.lge.networksettings/com.lge.networksettings.MobileNetworkSettingsForGLOBAL")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = ForceLteBg,
        titleContentColor = ForceLteText,
        title = {
            Text("Advanced Settings", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { action ->
                    OutlinedButton(
                        onClick = {
                            if (!isAuthorized) {
                                Toast.makeText(
                                    context,
                                    "Shizuku permission required.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@OutlinedButton
                            }
                            scope.launch {
                                ShellExecutor.exec(action.cmd)
                            }
                            onDismiss()
                        },
                        shape  = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForceLteAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForceLteText),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(action.label, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = ForceLteOutlined, fontWeight = FontWeight.Bold)
            }
        }
    )
}
