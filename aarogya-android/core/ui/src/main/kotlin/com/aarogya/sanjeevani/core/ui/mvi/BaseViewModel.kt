package com.aarogya.sanjeevani.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one screen-ViewModel shape every feature screen in either app
 * should use:
 * - [State] is a single immutable snapshot of everything the screen
 *   needs to render (a data class, or a sealed interface when the
 *   screen has genuinely distinct shapes -- loading/error/content --
 *   rather than optional fields on one shape).
 * - [Event] is a one-off effect the screen reacts to exactly once
 *   (navigate, show a snackbar) -- delivered through a [Channel] rather
 *   than [uiState] so it survives at most one collector and is never
 *   accidentally replayed on rotation/recomposition the way a
 *   [StateFlow] value would be.
 * - [Action] is the closed set of things the user (or the screen itself,
 *   e.g. "retry") can do, handled in exactly one place: [onAction].
 *
 * A screen never mutates [uiState] itself -- only this class does, via
 * [setState] -- and never constructs [Event]s except via [sendEvent].
 */
abstract class BaseViewModel<State, Event, Action>(
    initialState: State,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = mutableUiState.asStateFlow()

    /** The latest state, for code that needs to read it synchronously
     *  (e.g. before issuing a network call) rather than collecting it. */
    protected val currentState: State
        get() = mutableUiState.value

    private val eventChannel = Channel<Event>(Channel.BUFFERED)

    /** Collect once per screen instance (typically in a `LaunchedEffect`
     *  keyed on `Unit`) -- this is a [Channel], not a [StateFlow], so
     *  events are never replayed to a late or second collector. */
    val uiEvent: Flow<Event> = eventChannel.receiveAsFlow()

    /** Applies [reducer] to the current state and publishes the result.
     *  Always derives the next state from the current one (`State.() ->
     *  State`, i.e. a `copy { ... }`-style transform) rather than taking
     *  a plain replacement value, so two concurrent updates (e.g. a
     *  fast double-tap) can never silently clobber each other. */
    protected fun setState(reducer: State.() -> State) {
        mutableUiState.update(reducer)
    }

    /** Queues a one-off [Event] for the screen to react to. Safe to call
     *  from any coroutine context this ViewModel launches, including
     *  ones that outlive the screen that's currently collecting
     *  [uiEvent] -- the [Channel] simply holds it until the next
     *  collector arrives. */
    protected fun sendEvent(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    /** The single entry point for every [Action] the screen reports --
     *  deliberately not split into one function per action, so a screen
     *  reviewing "everything that can happen here" only ever has one
     *  function to read. */
    abstract fun onAction(action: Action)
}
