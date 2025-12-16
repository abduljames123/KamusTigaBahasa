package lat.pam.kamustigabahasa

data class Word(
    val id: String = "",       // document id Firestore
    val indo: String = "",
    val english: String = "",
    val arabic: String = "",
    val category: String = ""
)
