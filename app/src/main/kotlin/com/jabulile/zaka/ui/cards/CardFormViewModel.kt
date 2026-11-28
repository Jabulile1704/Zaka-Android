package com.jabulile.zaka.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jabulile.zaka.core.BarcodeSymbology
import com.jabulile.zaka.core.CardTint
import com.jabulile.zaka.core.LoyaltyCard
import com.jabulile.zaka.core.Scanned
import com.jabulile.zaka.data.LoyaltyCardRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardFormUiState(
    val name: String = "",
    val number: String = "",
    val symbology: BarcodeSymbology = BarcodeSymbology.CODE128,
    /** Once the user picks a type, typing must stop changing it under them. */
    val symbologyChosenByHand: Boolean = false,
    val tintHex: String = CardTint.palette[0],
    val editing: Boolean = false,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    /** What the number looks like, which is only a suggestion. */
    val detected: BarcodeSymbology get() = BarcodeSymbology.detected(number)

    /** A type that cannot draw this number would save a card that cannot be shown. */
    val drawable: Boolean get() = symbology.accepts(number)

    val canSave: Boolean get() = !saving && name.isNotBlank() && drawable
}

class CardFormViewModel(
    private val cards: LoyaltyCardRepository,
    private val cardId: Long?,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    /** Kept so that editing a card does not move it to the top of the list. */
    private var addedAt: Instant = clock.instant()

    private val _state = MutableStateFlow(
        CardFormUiState(editing = cardId != null, loaded = cardId == null),
    )
    val state: StateFlow<CardFormUiState> = _state.asStateFlow()

    init {
        if (cardId != null) load(cardId)
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val existing = cards.find(id)
            if (existing == null) {
                _state.update { it.copy(loaded = true, finished = true) }
                return@launch
            }
            addedAt = existing.addedAt
            _state.value = CardFormUiState(
                name = existing.name,
                number = existing.number,
                symbology = existing.symbology,
                // A saved card's type is a decision somebody already made.
                symbologyChosenByHand = true,
                tintHex = existing.tintHex,
                editing = true,
                loaded = true,
            )
        }
    }

    fun nameChanged(name: String) = _state.update { it.copy(name = name) }

    fun numberChanged(number: String) = _state.update { current ->
        current.copy(
            number = number,
            symbology = if (current.symbologyChosenByHand) {
                current.symbology
            } else {
                BarcodeSymbology.detected(number)
            },
        )
    }

    /**
     * A scan fills in the number and, when the scanner recognised the format,
     * fixes the type too — a reader that says "this was an EAN-13" is better
     * evidence than guessing from the digits, so it counts as chosen.
     */
    fun scanned(result: Scanned) = _state.update {
        it.copy(
            number = result.number,
            symbology = result.symbology ?: BarcodeSymbology.detected(result.number),
            symbologyChosenByHand = result.symbology != null,
        )
    }

    fun symbologyChosen(symbology: BarcodeSymbology) = _state.update {
        it.copy(symbology = symbology, symbologyChosenByHand = true)
    }

    fun tintChosen(hex: String) = _state.update { it.copy(tintHex = hex) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        if (!_state.compareAndSet(current, current.copy(saving = true))) return

        viewModelScope.launch {
            cards.save(
                LoyaltyCard(
                    id = cardId ?: 0,
                    name = current.name.trim(),
                    number = current.number.trim(),
                    symbology = current.symbology,
                    tintHex = current.tintHex,
                    addedAt = addedAt,
                ),
            )
            _state.update { it.copy(saving = false, finished = true) }
        }
    }

    fun delete() {
        val id = cardId ?: return
        viewModelScope.launch {
            cards.delete(id)
            _state.update { it.copy(finished = true) }
        }
    }

    fun reset() {
        addedAt = clock.instant()
        _state.value = CardFormUiState(loaded = true)
    }

    companion object {
        fun factory(
            cards: LoyaltyCardRepository,
            cardId: Long?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CardFormViewModel(cards, cardId) }
        }
    }
}
