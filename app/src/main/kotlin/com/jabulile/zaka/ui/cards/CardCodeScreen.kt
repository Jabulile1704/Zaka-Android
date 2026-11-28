package com.jabulile.zaka.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.R
import com.jabulile.zaka.core.CardDisplayMode
import com.jabulile.zaka.core.CodeGenerator
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.ui.components.CodeImage
import com.jabulile.zaka.ui.components.KeepScreenBrightFor

/**
 * One card, held up at a counter.
 *
 * Which forms are offered, and which one opens, are [CardDisplayMode]'s
 * decisions — the same tested rules the iOS app uses. A card saved as Code 128
 * is never promoted to EAN-13 because its digits happen to validate, and a
 * number that cannot be drawn as a linear barcode is only offered as a QR.
 */
@Composable
fun CardCodeScreen(
    card: LoyaltyCard,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenBrightFor()

    val offered = remember(card.number, card.symbology) {
        CardDisplayMode.available(card.number, card.symbology)
    }
    var mode by rememberSaveable(card.id) {
        mutableStateOf(CardDisplayMode.initial(card.number, card.symbology))
    }

    val symbology = mode.symbology(card.number, card.symbology)
    val code = remember(card.number, symbology) { CodeGenerator.of(card.number, symbology) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = card.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        if (code == null) {
            Text(
                text = stringResource(R.string.card_cannot_draw),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        } else {
            CodeImage(
                code = code,
                description = stringResource(
                    R.string.card_code_description,
                    symbology.label,
                    card.name,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // The number in text, for when the scanner gives up and the cashier
        // types it in.
        Text(
            text = card.number,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        if (offered.size > 1) {
            SingleChoiceSegmentedButtonRow {
                offered.forEachIndexed { index, candidate ->
                    SegmentedButton(
                        selected = mode == candidate,
                        onClick = { mode = candidate },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = offered.size,
                        ),
                        label = { Text(candidate.label) },
                    )
                }
            }
        }

        TextButton(onClick = onClose) {
            Text(stringResource(R.string.card_done))
        }
    }
}
