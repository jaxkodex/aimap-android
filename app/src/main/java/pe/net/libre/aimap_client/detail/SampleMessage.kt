package pe.net.libre.aimap_client.detail

import pe.net.libre.aimap_client.api.Labels
import pe.net.libre.aimap_client.api.Mailbox
import pe.net.libre.aimap_client.api.Message
import pe.net.libre.aimap_client.home.SampleHome

/**
 * Synthetic sample data from the Pencil read design. Every name, address and line is made up;
 * none of it is real mail. The previews use it, and debug builds can open it by hand.
 */
object SampleMessage {
    /** No real message has a negative id, so [DetailRoute] can tell this one from a fetch. */
    const val ID = -1L

    /** Four hours ago, so the holder reads "4h" like the design. */
    private val sent = SampleHome.now.minusHours(4).minusMinutes(5).toInstant()

    val message = Message(
        messageId = ID,
        account = "personal@example.com",
        sender = "Priya Nair",
        fromEmail = "priya@northwind-talent.example",
        subject = "Interview: Senior Data Engineer at Northwind",
        sentAt = sent,
        unread = true,
        flagged = false,
        bulk = false,
        inReplyTo = null,
        labels = Labels(
            importance = "high",
            actionBucket = "reply",
            tags = listOf("recruiting"),
            insight = null,
            needsReview = false,
            priority = 0.82,
            reasons = listOf("Asks you to do something", "Written to you by a person"),
            classifiedAt = sent.plusSeconds(120),
        ),
        mailboxes = listOf(Mailbox("INBOX", emptyList())),
        state = null,
    )

    // One line per paragraph: the API strips blank lines, and mail arrives flowed more often than wrapped.
    val body = listOf(
        "Thanks for applying to the Senior Data Engineer role. The team liked your profile, and we'd like " +
            "to bring you in for a first interview: 45 minutes on video with Tomás, our engineering lead.",
        "Could you send a few times that work for you next week? We're flexible between 10:00 and 17:00 " +
            "Berlin time.",
        "The interview is a walk through one of your past projects, then half an hour on how you would model " +
            "a slowly changing dimension for our billing warehouse. Nothing to prepare, nothing to install.",
        "If it helps I can send the role description again, with the salary band and the team's hours.",
        "Best,",
        "Priya",
    ).joinToString("\n")

    val read = Read.Ready(message.toUi(SampleHome.now), BodyState.Text(body))

    /** The same letter under another message's number, so the sample Bay can open any of its cards. */
    fun read(messageId: Long): Read.Ready =
        if (messageId == ID) read else Read.Ready(message.copy(messageId = messageId).toUi(SampleHome.now), BodyState.Text(body))
}
