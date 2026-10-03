package pe.net.libre.aimap_client.detail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.aimap_client.api.Labels
import java.time.Instant

/** The reply detection fallback, for when the service does not send a reply field. */
class ReplyDetectionTest {
    @Test
    fun reply_bucket_always_needs_a_reply() {
        val labels = labels("reply", bulk = false)
        assertTrue(needsReplyFallback(labels, bulk = false))
    }

    @Test
    fun reply_bucket_needs_reply_even_when_bulk() {
        val labels = labels("reply", bulk = false)
        assertTrue(needsReplyFallback(labels, bulk = true))
    }

    @Test
    fun act_now_on_non_bulk_needs_a_reply() {
        val labels = labels("act_now", bulk = false)
        assertTrue(needsReplyFallback(labels, bulk = false))
    }

    @Test
    fun act_now_on_bulk_does_not_need_a_reply() {
        val labels = labels("act_now", bulk = true)
        assertFalse(needsReplyFallback(labels, bulk = true))
    }

    @Test
    fun verify_does_not_need_a_reply() {
        val labels = labels("verify", bulk = false)
        assertFalse(needsReplyFallback(labels, bulk = false))
    }

    @Test
    fun null_labels_do_not_need_a_reply() {
        assertFalse(needsReplyFallback(null, bulk = false))
    }

    @Test
    fun unknown_bucket_does_not_need_a_reply() {
        val labels = labels("discard", bulk = false)
        assertFalse(needsReplyFallback(labels, bulk = false))
    }

    private fun labels(bucket: String, bulk: Boolean) = Labels(
        importance = "high",
        actionBucket = bucket,
        tags = emptyList(),
        insight = null,
        needsReview = false,
        priority = 0.8,
        reasons = emptyList(),
        classifiedAt = Instant.now(),
    )
}
