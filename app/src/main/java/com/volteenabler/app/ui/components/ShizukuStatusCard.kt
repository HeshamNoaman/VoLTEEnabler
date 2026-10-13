package com.volteenabler.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.volteenabler.app.R
import com.volteenabler.app.shizuku.ShizukuManager
import com.volteenabler.app.ui.theme.ErrorRed
import com.volteenabler.app.ui.theme.SuccessGreen
import com.volteenabler.app.ui.theme.WarningOrange

@Composable
fun ShizukuStatusCard(
    isInstalled: Boolean,
    isRunning: Boolean,
    isAuthorized: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (statusColor, statusIcon, statusText, descriptionText) = when {
        !isInstalled -> Quad(
            ErrorRed,
            Icons.Default.Error,
            stringResource(R.string.shizuku_not_installed),
            stringResource(R.string.shizuku_desc_not_running)
        )
        !isRunning -> Quad(
            ErrorRed,
            Icons.Default.Warning,
            stringResource(R.string.shizuku_not_running),
            stringResource(R.string.shizuku_desc_not_running)
        )
        !isAuthorized -> Quad(
            WarningOrange,
            Icons.Default.Warning,
            stringResource(R.string.shizuku_permission_needed),
            stringResource(R.string.error_shizuku_required)
        )
        else -> Quad(
            SuccessGreen,
            Icons.Default.CheckCircle,
            stringResource(R.string.shizuku_connected),
            stringResource(R.string.shizuku_desc_connected)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(statusColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.shizuku_status_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    !isInstalled -> {
                        Button(
                            onClick = { ShizukuManager.openShizukuWebsite(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                        ) {
                            Text(stringResource(R.string.shizuku_install_btn))
                        }
                    }
                    !isRunning -> {
                        Button(
                            onClick = { ShizukuManager.openShizukuApp(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                        ) {
                            Text(stringResource(R.string.shizuku_open_app_btn))
                        }
                    }
                    !isAuthorized -> {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                        ) {
                            Text(stringResource(R.string.shizuku_grant_btn))
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = { ShizukuManager.checkStatus() }
                        ) {
                            Text(stringResource(R.string.device_refresh))
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
