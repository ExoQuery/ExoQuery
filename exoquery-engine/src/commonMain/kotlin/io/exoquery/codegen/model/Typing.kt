package io.exoquery.codegen.model

sealed interface UnrecognizedTypeStrategy {
    data object AssumeString : UnrecognizedTypeStrategy
    data object SkipColumn : UnrecognizedTypeStrategy
    data object ThrowTypingError : UnrecognizedTypeStrategy
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
