package com.volteenabler.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.volteenabler.app.R
import com.volteenabler.app.shizuku.ShellExecutor
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    isAuthorized: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var commandInput by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("Shizuku ADB Shell ready.\nEnter command or tap presets below.\n") }
    var isRunning by remember { mutableStateOf(false) }

    val terminalScrollState = rememberScrollState()
    val terminalHScrollState = rememberScrollState()

    val presets = listOf(
        "getprop ro.product.model",
        "getprop gsm.sim.state",
        "settings get system lock_application_shortcut",
        "settings get global volte_vt_enabled",
        "pm list packages -u | grep ims",
        "dumpsys telephony.registry | grep -i ims"
    )

    fun executeCmd(cmd: String) {
        if (cmd.isBlank()) return
        isRunning = true
        scope.launch {
            val header = "\n$ $cmd\n"
            outputText += header
            val res = ShellExecutor.exec(cmd)
            val resultText = buildString {
                if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                if (res.stderr.isNotBlank()) append("[Error] ").append(res.stderr).append("\n")
                append("[Exit code: ${res.exitCode}]\n")
            }
            outputText += resultText
            isRunning = false
            terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.terminal_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Terminal Output", outputText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.terminal_copy), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.terminal_copy)
                    )
                }

                IconButton(
                    onClick = { outputText = "" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.terminal_clear)
                    )
                }
            }
        }

        // Preset command chips
        Text(
            text = stringResource(R.string.terminal_presets_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                SuggestionChip(
                    onClick = {
                        commandInput = preset
                        executeCmd(preset)
                    },
                    label = { Text(preset, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        // Terminal Output Screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(terminalScrollState)
                    .horizontalScroll(terminalHScrollState)
            ) {
                Text(
                    text = outputText,
                    color = Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Input field and send button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text(stringResource(R.string.terminal_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        executeCmd(commandInput)
                        commandInput = ""
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    executeCmd(commandInput)
                    commandInput = ""
                },
                enabled = !isRunning && commandInput.isNotBlank(),
                modifier = Modifier.height(56.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.terminal_run)
                    )
                }
            }
        }
    }
}
