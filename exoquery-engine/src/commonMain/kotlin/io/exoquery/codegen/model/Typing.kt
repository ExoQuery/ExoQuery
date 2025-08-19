package io.exoquery.codegen.model

import io.exoquery.annotation.ExoInternal
import kotlinx.serialization.Serializable as Ser

@Ser
sealed interface UnrecognizedTypeStrategy {
    @Ser data object AssumeString : UnrecognizedTypeStrategy
    @Ser data object SkipColumn : UnrecognizedTypeStrategy
    @Ser data object ThrowTypingError : UnrecognizedTypeStrategy

    @ExoInternal
    companion object {
    }
}

sealed interface NumericPreference {
    data object PreferPrimitivesWhenPossible : NumericPreference
    data object UseDefaults : NumericPreference
}

class TypingError(override val message: String) : RuntimeException(message)

//sealed trait UnrecognizedTypeStrategy
//case object AssumeString     extends UnrecognizedTypeStrategy
//case object SkipColumn       extends UnrecognizedTypeStrategy
//case object ThrowTypingError extends UnrecognizedTypeStrategy
//

//sealed trait NumericPreference
//case object PreferPrimitivesWhenPossible extends NumericPreference
//case object UseDefaults                  extends NumericPreference
//
//class TypingError(private val message: String) extends RuntimeException(message) {}
