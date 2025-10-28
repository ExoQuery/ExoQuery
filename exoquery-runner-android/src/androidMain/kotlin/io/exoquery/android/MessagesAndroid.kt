package io.exoquery.android

// @formatter:off
object MessagesAndroid {
val SqliteNativeCantReturningKeysIfNotInsert =
    """
In Sqlite for Android returningKeys can only be used for inserts. If you want to return a value from a query use the `returning` method to define a proper SQL RETURNING Clause.

For example, instead of this:
val query = sql { update<Person> { set(firstName to "Joe") } }.where { lastName == "Bloggs" }.returningKeys { id }

You can do this:
val query = sql { update<Person> { set(firstName to "Joe") } }.where { lastName == "Bloggs" }.returning { id }

This will become:
UPDATE Person SET firstName = ? WHERE lastName = ? RETURNING id

See here for more details: https://sqlite.org/lang_returning.html
""".trimIndent()
}
// @formatter:on
