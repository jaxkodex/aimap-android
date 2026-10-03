package pe.net.libre.aimap_client.detail

import pe.net.libre.aimap_client.api.Draft

/**
 * Builds a mailto: URI for opening a draft in a mail app.
 * Returns the complete URI as a string with proper percent-encoding.
 * Spaces become %20, not +, as required by the mailto: scheme.
 */
fun buildMailtoUri(draft: Draft): String {
    val to = draft.to.joinToString(",")
    val params = mutableListOf<String>()
    params.add("subject=${percentEncode(draft.subject)}")
    params.add("body=${percentEncode(draft.body)}")
    if (draft.cc.isNotEmpty()) {
        val cc = draft.cc.joinToString(",")
        params.add("cc=${percentEncode(cc)}")
    }
    return "mailto:$to?${params.joinToString("&")}"
}

/**
 * Percent-encodes a string for use in a URI query parameter.
 * Spaces become %20 (not +), which is required for mailto: URIs.
 */
private fun percentEncode(s: String): String = buildString(s.length * 2) {
    for (char in s) {
        when {
            char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' -> append(char)
            char in "-_.~" -> append(char)
            else -> append("%${char.code.toString(16).uppercase().padStart(2, '0')}")
        }
    }
}
