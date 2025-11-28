package io.exoquery.testdata

import io.exoquery.annotation.ExoEntity
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)

@JvmInline
@Serializable
value class PersonId(val value: Int)

// TODO when I remove @Contextual terpal-sql says Bad value "Bloggs" for column of type int which means the cursor
//      goes too far. Need to update terpal driver to handle value classes.
@Serializable
@ExoEntity("person")
data class PersonWithIdCtx(@Contextual val id: PersonId, val firstName: String, val lastName: String, val age: Int)

@Serializable
@ExoEntity("person")
data class PersonWithId(val id: PersonId, val firstName: String, val lastName: String, val age: Int)

@Serializable
@ExoEntity("person")
data class PersonNullable(val id: Int, val firstName: String?, val lastName: String?, val age: Int?)

@Serializable
data class Address(val ownerId: Int, val street: String, val zip: Int)

@Serializable
@ExoEntity("address")
data class AddressWithId(val ownerId: PersonId, val street: String, val zip: Int)

@Serializable
@ExoEntity("address")
data class AddressWithIdCtx(val ownerId: PersonId, val street: String, val zip: Int)

@Serializable
data class Robot(val ownerId: Int, val model: String, val age: Int)
