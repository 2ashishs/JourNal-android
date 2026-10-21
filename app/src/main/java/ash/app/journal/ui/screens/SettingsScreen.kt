package ash.app.journal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ash.app.journal.R
import ash.app.journal.settings.SnoozeDuration
import ash.app.journal.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val currentSnooze by viewModel.snoozeDuration.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDisablePinConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SECTION 1: NOTIFICATIONS ---
            Text(
                text = "Notifications & Reminders",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            SettingsItemRow(
                icon = R.drawable.ic_alarm,
                title = "Default Snooze Duration",
                subtitle = currentSnooze.label,
                onClick = { showSnoozeDialog = true }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            // --- SECTION 2: SECURITY & PRIVACY ---
            Text(
                text = "Security & Privacy",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            SettingsItemRow(
                icon = R.drawable.ic_lock,
                title = if (isPinSet) "Change Vault PIN" else "Set Vault PIN",
                subtitle = if (isPinSet) "PIN Protection is active" else "Protect private notes with a 4-digit PIN",
                onClick = { showPinDialog = true }
            )

            if (isPinSet) {
                SettingsItemRow(
                    icon = R.drawable.ic_visibility_off,
                    title = "Remove Vault PIN",
                    subtitle = "Disable PIN authentication",
                    onClick = { showDisablePinConfirmDialog = true }
                )
            }
        }
    }

    // --- SNOOZE SELECTION DIALOG ---
    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Select Snooze Duration") },
            text = {
                Column {
                    SnoozeDuration.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.onSnoozeDurationSelected(option)
                                    showSnoozeDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSnooze == option,
                                onClick = {
                                    viewModel.onSnoozeDurationSelected(option)
                                    showSnoozeDialog = false
                                }
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- SET / CHANGE PIN DIALOG ---
    if (showPinDialog) {
        PinSetupDialog(
            isPinAlreadySet = isPinSet,
            onVerifyOldPin = { pin -> viewModel.verifyPin(pin) },
            onSaveNewPin = { newPin ->
                viewModel.onSetPin(newPin)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }

    // --- DISABLE PIN DIALOG ---
    if (showDisablePinConfirmDialog) {
        var enteredPin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showDisablePinConfirmDialog = false },
            title = { Text("Remove PIN Protection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current PIN to disable vault security:")
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                enteredPin = it
                                isError = false
                            }
                        },
                        isError = isError,
                        label = { Text("Current PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (isError) {
                        Text(
                            text = "Incorrect PIN",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.verifyPin(enteredPin)) {
                                viewModel.onRemovePin()
                                showDisablePinConfirmDialog = false
                            } else {
                                isError = true
                            }
                        }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_search_again),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PinSetupDialog(
    isPinAlreadySet: Boolean,
    onVerifyOldPin: suspend (String) -> Boolean,
    onSaveNewPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(if (isPinAlreadySet) 1 else 2) } // 1: Old PIN, 2: New PIN, 3: Confirm PIN
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    1 -> "Enter Current PIN"
                    2 -> "Enter New 4-Digit PIN"
                    else -> "Confirm New PIN"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentText = when (step) {
                    1 -> oldPin
                    2 -> newPin
                    else -> confirmPin
                }

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            errorMessage = null
                            when (step) {
                                1 -> oldPin = input
                                2 -> newPin = input
                                3 -> confirmPin = input
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorMessage != null
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (step) {
                        1 -> {
                            scope.launch {
                                if (onVerifyOldPin(oldPin)) {
                                    step = 2
                                } else {
                                    errorMessage = "Incorrect current PIN"
                                }
                            }
                        }
                        2 -> {
                            if (newPin.length == 4) {
                                step = 3
                            } else {
                                errorMessage = "PIN must be 4 digits"
                            }
                        }
                        3 -> {
                            if (confirmPin == newPin) {
                                onSaveNewPin(newPin)
                            } else {
                                errorMessage = "PINs do not match"
                            }
                        }
                    }
                }
            ) {
                Text(if (step == 3) "Save" else "Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
