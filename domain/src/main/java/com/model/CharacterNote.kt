package com.model

data class CharacterNote(val id: Long, val bookId: String, val name: String, val description: String, val firstPositionMs: Long?) {
    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 1_000
    }
}
