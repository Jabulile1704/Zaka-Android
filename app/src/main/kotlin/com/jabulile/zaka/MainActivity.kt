package com.jabulile.zaka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jabulile.zaka.core.ExpenseCategory
import com.jabulile.zaka.ui.theme.ZakaTheme
import com.jabulile.zaka.ui.theme.color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZakaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
                    Scaffolding(modifier = Modifier.padding(insets))
                }
            }
        }
    }
}

/**
 * What sprint 1 has to show: the theme, and the category palette coming out of
 * the shared logic module. The Expenses, Add, Insights and Cards screens land
 * in sprint 2 and replace this.
 */
@Composable
private fun Scaffolding(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.scaffold_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.scaffold_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CategorySwatches()
    }
}

@Composable
private fun CategorySwatches(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics {
            contentDescription = "The sixteen expense category colours"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ExpenseCategory.entries.forEach { category ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(category.color, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldingPreview() {
    ZakaTheme(dynamicColor = false) {
        Scaffolding()
    }
}
