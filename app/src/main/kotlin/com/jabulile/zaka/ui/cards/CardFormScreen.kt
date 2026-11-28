package com.jabulile.zaka.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.R
import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.CardTint
import com.jabulile.zaka.core.CodeGenerator
import com.jabulile.zaka.ui.components.CodeImage
import com.jabulile.zaka.ui.components.ZakaIcons
import com.jabulile.zaka.ui.theme.hexColor

/**
 * Add or edit a card. Same form, two modes.
 *
 * The barcode type follows the number as it is typed — thirteen valid digits
 * read as EAN-13, anything else as Code 128 — until the user picks one, at
 * which point it stops moving under them.
 */
@Composable
fun CardFormScreen(
    state: CardFormUiState,
    onName: (String) -> Unit,
    onNumber: (String) -> Unit,
    onSymbology: (BarcodeSymbology) -> Unit,
    onTint: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onScan: (() -> Unit)? = null,
) {
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(state.loaded) {
        if (state.loaded && !state.editing) nameFocus.requestFocus()
    }

    val preview = remember(state.number, state.symbology) {
        CodeGenerator.of(state.number, state.symbology)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onName,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocus),
            label = { Text(stringResource(R.string.card_name)) },
            singleLine = true,
        )

        OutlinedTextField(
            value = state.number,
            onValueChange = onNumber,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.card_number)) },
            singleLine = true,
            isError = state.number.isNotEmpty() && !state.drawable,
            supportingText = {
                Text(
                    if (state.number.isEmpty()) {
                        stringResource(R.string.card_number_hint)
                    } else if (!state.drawable) {
                        stringResource(R.string.card_number_undrawable, state.symbology.label)
                    } else {
                        state.symbology.hint
                    },
                )
            },
        )

        if (onScan != null) {
            OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = ZakaIcons.Scan,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.card_scan))
            }
        }

        Text(
            text = stringResource(R.string.card_type),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BarcodeSymbology.entries.forEach { candidate ->
                FilterChip(
                    selected = state.symbology == candidate,
                    onClick = { onSymbology(candidate) },
                    label = { Text(candidate.label) },
                    enabled = candidate.accepts(state.number),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = stringResource(R.string.card_colour),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        TintPicker(chosen = state.tintHex, onTint = onTint)

        if (preview != null) {
            CodeImage(
                code = preview,
                description = stringResource(R.string.card_preview_description),
                modifier = Modifier.fillMaxWidth(),
                barHeight = 90.dp,
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSave,
        ) {
            Text(
                stringResource(if (state.editing) R.string.form_update else R.string.form_save),
            )
        }

        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.form_cancel))
        }

        if (state.editing) {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.card_delete_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TintPicker(
    chosen: String,
    onTint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardTint.palette.forEach { hex ->
            val selected = hex == chosen
            val label = stringResource(R.string.card_colour_swatch, hex)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(hex.hexColor(), CircleShape)
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    )
                    .clickable { onTint(hex) }
                    .semantics {
                        contentDescription = label
                        this.selected = selected
                    },
            )
        }
    }
}
