package com.kamsiob.healthtrail.i18n

/**
 * Joining pieces of text so a right to left layout cannot rearrange them.
 *
 * **This is a defect the app had everywhere and could only be seen in Arabic.**
 * Five screens build a line by joining parts with a middle dot: a value and its
 * unit and a date, a role and a phone number, a count and a sum. In English they
 * read as written. In Arabic they do not.
 *
 * Found on Progress. `1.4 0 to 10 · 26 يونيو 2026` rendered as
 * `2026 يونيو to 10 · 26 0 1.4`: the Latin and numeric runs and the Arabic
 * month name are separate bidirectional runs, and the Unicode algorithm
 * reorders them relative to each other because nothing told it they are
 * separate things that happen to sit side by side.
 *
 * **The fix is isolation, not a direction mark.** Each part is wrapped in
 * `FIRST STRONG ISOLATE` and `POP DIRECTIONAL ISOLATE`, which tells the
 * algorithm to lay that part out on its own and treat the whole of it as one
 * neutral object in the line around it. Each piece keeps its own internal
 * direction, and the pieces stay in the order the code put them.
 *
 * **Not `‏` or `‎`.** A direction mark pushes on the runs around it
 * and its effect depends on what is next to it, so a line that is correct today
 * breaks when somebody's unit is a word rather than a number. An isolate is
 * scoped and does not care what surrounds it.
 *
 * **Invisible, and it stays that way.** These are formatting characters with no
 * width. They never reach the readable copy in an archive or the export, both of
 * which build their own text from the same data rather than from these strings.
 *
 * **They do reach the accessibility tree, and that is stated rather than
 * assumed.** Dumping the semantics of a screen using this shows the characters
 * present in the node text. Both are Unicode `Cf`, default ignorable by the
 * standard, which is precisely the category a text to speech engine and a
 * braille display are specified to skip, and Android's own `BidiFormatter`
 * emits them into text that goes to the same places. **It has not been heard
 * with the reader on**, which is what #224 is for. It is kept meanwhile because
 * the alternative is a certain defect for anybody reading right to left, rather
 * than a documented uncertainty for anybody using a reader.
 */
object Bidi {

    private const val FIRST_STRONG_ISOLATE = '⁨'
    private const val POP_DIRECTIONAL_ISOLATE = '⁩'

    /** The separator this app uses between parts of one line. */
    const val DOT = " · "

    /**
     * One part, laid out on its own terms.
     *
     * Use it wherever a value composed elsewhere is dropped into a sentence, for
     * example a person's name inside a question or a file name inside a warning.
     */
    fun isolate(text: String): String = "$FIRST_STRONG_ISOLATE$text$POP_DIRECTIONAL_ISOLATE"

    /**
     * Joins the parts that are actually there, each isolated, with [separator].
     *
     * Blank and null parts are dropped rather than producing a line that begins
     * or ends with a dangling separator, which is what every caller was already
     * doing by hand.
     */
    fun join(vararg parts: String?, separator: String = DOT): String =
        parts.filterNot { it.isNullOrBlank() }.joinToString(separator) { isolate(it!!) }

    /** The same, for a list built up rather than known at the call site. */
    fun join(parts: List<String?>, separator: String = DOT): String =
        parts.filterNot { it.isNullOrBlank() }.joinToString(separator) { isolate(it!!) }
}
