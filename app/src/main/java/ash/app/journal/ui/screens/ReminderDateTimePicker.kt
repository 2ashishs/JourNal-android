package ash.app.journal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDateTimePickerDialog(
    initialTimestamp: Long? = null,
    onDismiss: () -> Unit,
    onDateTimeSelected: (Long) -> Unit
) {
    var step by remember { mutableStateOf(PickerStep.DATE) }

    val initialCalendar = remember {
        Calendar.getInstance().apply {
            if (initialTimestamp != null && initialTimestamp > System.currentTimeMillis()) {
                timeInMillis = initialTimestamp
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialCalendar.timeInMillis
    )

    val timePickerState = rememberTimePickerState(
        initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    when (step) {
        PickerStep.DATE -> {
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = { step = PickerStep.TIME }) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        PickerStep.TIME -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TimePicker(state = timePickerState)

                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { step = PickerStep.DATE }) {
                                Text("Back")
                            }
                            TextButton(onClick = {
                                val selectedDateMillis =
                                    datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                }
                                onDateTimeSelected(cal.timeInMillis)
                            }) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class PickerStep {
    DATE, TIME
}
