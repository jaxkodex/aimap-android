package pe.net.libre.aimap_client.detail

import pe.net.libre.aimap_client.api.Labels

/**
 * Fallback heuristic for reply detection when the service does not send a `reply` field.
 * The rule is: action_bucket is "reply", or action_bucket is "act_now" on a non-bulk message.
 * This matches the service's rule as of the wire contract, kept local so older services keep working.
 */
fun needsReplyFallback(labels: Labels?, bulk: Boolean): Boolean {
    if (labels == null) return false
    return when (labels.actionBucket) {
        "reply" -> true
        "act_now" -> !bulk
        else -> false
    }
}
