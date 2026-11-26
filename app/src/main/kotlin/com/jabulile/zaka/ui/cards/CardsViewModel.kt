package com.jabulile.zaka.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.CsvExport
import com.jabulile.zaka.core.CsvFile
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.data.LoyaltyCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardsUiState(
    val cards: List<LoyaltyCard> = emptyList(),
    val loaded: Boolean = false,
    val export: CsvFile? = null,
)

class CardsViewModel(
    private val cards: LoyaltyCardRepository,
) : ViewModel() {
    private val pending = MutableStateFlow<CsvFile?>(null)

    val state: StateFlow<CardsUiState> = combine(cards.observeAll(), pending) { all, export ->
        CardsUiState(cards = all, loaded = true, export = export)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CardsUiState(),
        )

    fun delete(id: Long) {
        viewModelScope.launch { cards.delete(id) }
    }

    /** The cards as CSV. The iOS export has never included them. */
    fun export() {
        pending.value = CsvExport.cards(state.value.cards)
    }

    fun exportShared() {
        pending.value = null
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(cards: LoyaltyCardRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CardsViewModel(cards) }
        }
    }
}
