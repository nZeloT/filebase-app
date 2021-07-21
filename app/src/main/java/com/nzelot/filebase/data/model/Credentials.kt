package com.nzelot.filebase.data.model

data class Credentials(
    val username: String = "",
    val password: CharArray = CharArray(0),
    val domain: String = ""
) {
    fun isComplete() : Boolean = username.isNotBlank() && domain.isNotBlank()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Credentials

        if (username != other.username) return false
        if (domain != other.domain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + domain.hashCode()
        return result
    }

    override fun toString(): String {
        return "Credentials(username='$username', password=*******, domain='$domain')"
    }


}
