package de.dirkgerhardt.bahnfrei.data

data class ChancenInfo(
    val chancen: MedaillenChancen,
    val medaille: Medaille?,
    val text: String,
    val motivationsSpruchIndex: Int?
)