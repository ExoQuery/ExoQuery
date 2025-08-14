package io.exoquery.public

import io.exoquery.annotation.ExoValue
import kotlinx.serialization.SerialName

@SerialName("person")
data class Person(val id: Int, val name: String, val age: Int)