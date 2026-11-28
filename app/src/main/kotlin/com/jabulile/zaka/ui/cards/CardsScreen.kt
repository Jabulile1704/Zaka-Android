package com.jabulile.zaka.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.R
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.ui.components.ZakaIcons
import com.jabulile.zaka.ui.theme.hexColor

/**
 * The cards, newest first. Tap one to hold it up at a till; tap the row at the
 * end to add another.
 */
@Composable
fun CardsScreen(
    state: CardsUiState,
    onShow: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loaded && state.cards.isEmpty()) {
            item { EmptyCards() }
        }

        items(items = state.cards, key = { it.id }) { card ->
            CardTile(
                card = card,
                onShow = { onShow(card.id) },
                onEdit = { onEdit(card.id) },
                onDelete = { onDelete(card.id) },
            )
        }

        item { AddCardRow(onAdd = onAdd) }
    }
}

@Composable
private fun CardTile(
    card: LoyaltyCard,
    onShow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.card_edit_action)
    val deleteLabel = stringResource(R.string.card_delete_action)

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.card_show_action),
                onClick = onShow,
            )
            // Editing and deleting are a long-press menu elsewhere; here they
            // are explicit actions, so neither needs a gesture to reach.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(editLabel) { onEdit(); true },
                    CustomAccessibilityAction(deleteLabel) { onDelete(); true },
                )
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 64.dp)
                    .background(
                        card.tintHex.hexColor(),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = card.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (card.lastFour.isEmpty()) {
                        card.symbology.label
                    } else {
                        stringResource(R.string.card_last_four, card.lastFour)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = ZakaIcons.Next,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun AddCardRow(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ZakaIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.card_add),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EmptyCards(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.cards_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.cards_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
