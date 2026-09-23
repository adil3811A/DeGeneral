package com.adll.de_general.feature.journal.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.adll.de_general.feature.journal.domain.canPickMillis
import com.adll.de_general.feature.journal.domain.dateToPickerMillis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * Material's date picker, bounded to days that have happened.
 *
 * Shared by the composer (backdating an entry) and the archive (filtering by day). Past days only
 * is right for both: an entry cannot be filed on a day that has not happened, so there is nothing
 * on one to filter for either.
 *
 * Two caveats worth knowing before touching this. It brings its own typography opinions, which will
 * not match Newsreader — checked against material3 1.12.0-alpha03, where the picker is no longer
 * `@ExperimentalMaterial3Api`, so no opt-in is needed. And `selectedDateMillis` is **UTC midnight**
 * of the chosen day, not a local instant — so [onPicked] hands back that raw value and the
 * conversion lives in `JournalDates.kt`. It must not be done by reading it in the device's zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDatePickerDialog(
    initialDate: LocalDate,
    nowMillis: Long,
    zone: TimeZone,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onPicked: (Long?) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateToPickerMillis(initialDate),
        selectableDates = remember(nowMillis, zone) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    canPickMillis(utcTimeMillis, nowMillis, zone)
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(pickerState.selectedDateMillis) }) {
                Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
