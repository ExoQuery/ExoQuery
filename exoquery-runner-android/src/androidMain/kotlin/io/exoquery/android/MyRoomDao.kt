@file:ExoRoomInterface
package io.exoquery.android

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.exoquery.annotation.ExoRoomInterface
import io.exoquery.sql

@Entity(tableName = "Person")
data class PersonRoom(@PrimaryKey(autoGenerate = true) val id: Int = 0, val firstName: String, val lastName: String, val age: Int)

val joes =
  sql.select {
    val p = from(Table<PersonRoom>())
    where { p.firstName == paramRoom<String>("myFirstName") }
    p
  }.buildFor.Room("joes")
