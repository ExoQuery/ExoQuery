package io.exoquery.public

import io.exoquery.annotation.ExoValue
import kotlinx.serialization.SerialName

@SerialName("address")
data class Address(@SerialName("ownerid") val ownerId: Int, val street: String, val zip: String)