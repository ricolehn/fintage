package io.github.ricolehn.fintage.ui

import io.github.ricolehn.fintage.data.jellyfin.JellyfinException
import java.io.IOException
import kotlinx.serialization.SerializationException

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

/** A short, human message for a failed request. */
fun Throwable.friendlyMessage(): String = when (this) {
    is JellyfinException -> message ?: "The server answered with an error."
    is SerializationException -> "The server sent an answer Fintage does not understand."
    is IOException -> "Fintage cannot reach your server. Check your connection and try again."
    else -> message ?: "Something went wrong."
}
