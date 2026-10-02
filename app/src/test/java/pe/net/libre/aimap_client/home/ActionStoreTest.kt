package pe.net.libre.aimap_client.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.net.libre.aimap_client.api.ApiException
import pe.net.libre.aimap_client.api.MessageState

/** The optimistic store: the Bay moves first, the call either confirms it or puts it back. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActionStoreTest {
    private class Wire(private val fail: Exception? = null) {
        val calls = mutableListOf<Pair<Long, HomeAction>>()

        suspend fun write(messageId: Long, action: HomeAction) {
            calls += messageId to action
            fail?.let { throw it }
        }
    }

    private fun TestScope.store(wire: Wire, reconciles: MutableList<Unit> = mutableListOf()) =
        ActionStore(this, wire::write, reconcile = { reconciles += Unit }).also { it.show(SampleHome.home) }

    @Test
    fun handled_leaves_the_bay_before_the_call_answers() = runTest {
        val wire = Wire()
        val store = store(wire)

        store.act(1, HomeAction.Handled)
        val acting = store.state.value
        assertEquals(listOf(2L), acting.home?.actNow?.map { it.messageId })
        assertEquals(1, acting.home?.brief?.actNow)
        assertNull(acting.undo)
        assertEquals(emptyList<Pair<Long, HomeAction>>(), wire.calls)

        advanceUntilIdle()
        assertEquals(listOf(1L to HomeAction.Handled), wire.calls)
    }

    @Test
    fun a_successful_handled_offers_undo_for_five_seconds_and_reconciles() = runTest {
        val reconciles = mutableListOf<Unit>()
        val store = store(Wire(), reconciles)

        store.act(1, HomeAction.Handled)
        // Only as far as the call's answer: advancing to idle would run the five seconds out too.
        runCurrent()
        assertEquals(UndoOffer(1, "Handled"), store.state.value.undo)
        assertEquals(1, reconciles.size)

        advanceTimeBy(UNDO_MILLIS - 1)
        assertNotNull(store.state.value.undo)
        advanceTimeBy(2)
        assertNull(store.state.value.undo)
    }

    @Test
    fun undo_puts_the_card_back_where_it_was() = runTest {
        val wire = Wire()
        val store = store(wire)

        store.act(1, HomeAction.Handled)
        advanceUntilIdle()
        store.act(1, HomeAction.Undo)

        assertEquals(SampleHome.home, store.state.value.home)
        assertNull(store.state.value.undo)
        advanceUntilIdle()
        assertEquals(listOf(1L to HomeAction.Handled, 1L to HomeAction.Undo), wire.calls)
    }

    @Test
    fun undo_without_a_handled_behind_it_just_drops_the_later() = runTest {
        val store = store(Wire())

        store.act(3, HomeAction.Later)
        advanceUntilIdle()
        assertEquals(MessageState.Later, store.state.value.home?.waiting?.last()?.state)

        store.act(3, HomeAction.Undo)
        advanceUntilIdle()
        assertEquals(SampleHome.home.waiting.map { it.messageId }, store.state.value.home?.waiting?.map { it.messageId })
    }

    @Test
    fun a_failed_action_rolls_back_and_says_why() = runTest {
        val store = store(Wire(ApiException(404, "no such message")))

        store.act(1, HomeAction.Handled)
        assertEquals(listOf(2L), store.state.value.home?.actNow?.map { it.messageId })

        advanceUntilIdle()
        assertEquals(SampleHome.home, store.state.value.home)
        assertEquals("aimap has no such message any more.", store.state.value.error)
        assertNull(store.state.value.undo)

        store.clearError()
        assertNull(store.state.value.error)
    }

    @Test
    fun a_failed_later_rolls_back_too() = runTest {
        val store = store(Wire(ApiException(422, "bad action")))

        store.act(3, HomeAction.Later)
        assertEquals(listOf(4L, 5L, 3L), store.state.value.home?.waiting?.map { it.messageId })

        advanceUntilIdle()
        assertEquals(SampleHome.home, store.state.value.home)
        assertEquals("aimap would not take that action.", store.state.value.error)
    }

    @Test
    fun an_action_with_no_bay_yet_does_nothing() = runTest {
        val wire = Wire()
        val store = ActionStore(this, wire::write)

        store.act(1, HomeAction.Handled)
        advanceUntilIdle()
        assertEquals(emptyList<Pair<Long, HomeAction>>(), wire.calls)
    }

    @Test
    fun a_fresh_home_replaces_what_the_actions_left() = runTest {
        val store = store(Wire())

        store.act(1, HomeAction.Handled)
        runCurrent()
        store.show(SampleHome.home)
        assertEquals(SampleHome.home, store.state.value.home)
        // The offer stands: the refresh reconciles the Bay, it does not take the undo away.
        assertEquals(UndoOffer(1), store.state.value.undo)
    }

    @Test
    fun the_reason_names_what_went_wrong() {
        assertEquals("This account is not allowed to act on aimap.", reason(ApiException(403, "forbidden")))
        assertEquals("aimap said: it broke", reason(ApiException(500, "it broke")))
        assertEquals("Couldn't reach aimap. timed out", reason(java.io.IOException("timed out")))
    }
}
