package com.jabulile.zaka.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.data.LoyaltyCardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardsUiState(
    val cards: List<LoyaltyCard> = emptyList(),
    val loaded: Boolean = false,
)

class CardsViewModel(
    private val cards: LoyaltyCardRepository,
) : ViewModel() {
    val state: StateFlow<CardsUiState> = cards.observeAll()
        .map { CardsUiState(cards = it, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CardsUiState(),
        )

    fun delete(id: Long) {
        viewModelScope.launch { cards.delete(id) }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(cards: LoyaltyCardRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CardsViewModel(cards) }
        }
    }
}
