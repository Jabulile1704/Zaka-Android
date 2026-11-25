package com.jabulile.zaka.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.R
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.core.Money
import com.jabulile.zaka.ui.components.ZakaIcons
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Amount, date, note, category. That is the whole of data entry, and the
 * amount field takes focus as the screen opens so the keypad is already up.
 */
@Composable
fun ExpenseFormScreen(
    state: ExpenseFormUiState,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    val amountFocus = remember { FocusRequester() }
    var picking by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.loaded) {
        if (state.loaded) amountFocus.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.amountText,
            onValueChange = onAmountChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocus),
            label = { Text(stringResource(R.string.form_amount)) },
            prefix = { Text(Money.SYMBOL) },
            textStyle = MaterialTheme.typography.headlineMedium,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = { Text(stringResource(R.string.form_amount_hint)) },
        )

        DateField(date = state.date, onPick = { picking = true })

        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.form_note)) },
            singleLine = true,
        )

        Text(
            text = stringResource(R.string.form_category),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        CategoryGrid(selected = state.category, onChoose = onCategoryChange)

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSave,
        ) {
            Text(
                stringResource(
                    if (state.editing) R.string.form_update else R.string.form_save,
                ),
            )
        }

        if (onCancel != null) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.form_cancel))
            }
        }

        if (state.editing) {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.form_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (picking) {
        DateChooser(
            date = state.date,
            onPicked = {
                onDateChange(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun DateField(
    date: LocalDate,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE d MMM yyyy") }
    OutlinedButton(onClick = onPick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.form_date))
            Text(date.format(formatter))
        }
    }
}

/**
 * The date picker works in UTC milliseconds, so the local date has to be
 * carried across at UTC midnight and read back the same way. Using the device
 * zone here would shift the selection by a day either side of midnight.
 */
@Composable
private fun DateChooser(
    date: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = picker.selectedDateMillis
                    if (millis != null) {
                        onPicked(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.form_date_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.form_cancel))
            }
        },
    ) {
        DatePicker(state = picker)
    }
}

@Composable
private fun CategoryGrid(
    selected: ExpenseCategory,
    onChoose: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Two per row, rather than a lazy grid: sixteen chips do not need laziness,
    // and a lazy grid inside a scrolling column is a fight not worth having.
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpenseCategory.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    FilterChip(
                        selected = category == selected,
                        onClick = { onChoose(category) },
                        label = { Text(category.label) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                imageVector = ZakaIcons.of(category),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
