package pe.net.libre.aimap_client.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.net.libre.aimap_client.api.ApiException
import kotlin.coroutines.cancellation.CancellationException

/** How long the undo bar stays up after a message is handled. */
const val UNDO_MILLIS = 5_000L

/** The offer to put the last handled message back. */
data class UndoOffer(val messageId: Long, val text: String = "Handled")

/** The Bay as the screens see it while an action is in flight. */
data class Acting(
    val home: Home? = null,
    val undo: UndoOffer? = null,
    /** An action failed. The Bay is already back as it was; this says why. */
    val error: String? = null,
)

/**
 * The one write, applied before the API answers: Handled takes the card off the Bay, Later sends it
 * to the end of its section, Undo puts a handled one back. A call that fails restores the Bay the
 * action started from and names the reason, and a call that works is reconciled with a fresh
 * `GET /home`. v4, v5 and the read screen all act through one of these.
 */
class ActionStore(
    private val scope: CoroutineScope,
    private val write: suspend (Long, HomeAction) -> Unit,
    /** Called after a write went through, to reconcile with the API quietly. */
    private val reconcile: () -> Unit = {},
    private val undoMillis: Long = UNDO_MILLIS,
) {
    private val _state = MutableStateFlow(Acting())
    val state: StateFlow<Acting> = _state.asStateFlow()

    /** The Bay before the last Handled, so Undo can put the card back where it was. */
    private var undone: Pair<Long, Home>? = null
    private var waving: Job? = null

    /** A fresh `GET /home`, or null on sign-out. It replaces whatever the actions left. */
    fun show(home: Home?) {
        _state.update { it.copy(home = home) }
    }

    /** The error bar was dismissed. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun act(messageId: Long, action: HomeAction) {
        val before = _state.value.home ?: return
        val after = when (action) {
            HomeAction.Handled -> before.handled(messageId)
            HomeAction.Later -> before.later(messageId)
            // The Bay from before the Handled if it is still known, else just drop any "later".
            HomeAction.Undo -> undone?.takeIf { it.first == messageId }?.second ?: before.normal(messageId)
        }
        waving?.cancel()
        undone = if (action == HomeAction.Handled) messageId to before else null
        _state.value = Acting(home = after)
        scope.launch {
            try {
                write(messageId, action)
                if (action == HomeAction.Handled) offerUndo(messageId)
                reconcile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                undone = null
                _state.value = Acting(home = before, error = reason(e))
            }
        }
    }

    private fun offerUndo(messageId: Long) {
        _state.update { it.copy(undo = UndoOffer(messageId)) }
        waving = scope.launch {
            delay(undoMillis)
            _state.update { if (it.undo?.messageId == messageId) it.copy(undo = null) else it }
        }
    }
}

/** Why the action did not take, in the words the Bay's error bar uses. */
internal fun reason(e: Exception): String = when {
    e !is ApiException -> "Couldn't reach aimap. ${e.message.orEmpty()}".trim()
    e.status == 404 -> "aimap has no such message any more."
    e.status == 422 -> "aimap would not take that action."
    e.status == 403 -> "This account is not allowed to act on aimap."
    else -> "aimap said: ${e.message}"
}
