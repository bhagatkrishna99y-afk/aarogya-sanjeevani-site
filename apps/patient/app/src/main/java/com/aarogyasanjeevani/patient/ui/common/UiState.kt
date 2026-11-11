package com.aarogyasanjeevani.patient.ui.common

/** Generic load state for a screen's primary content. Every async screen renders all three branches. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}
