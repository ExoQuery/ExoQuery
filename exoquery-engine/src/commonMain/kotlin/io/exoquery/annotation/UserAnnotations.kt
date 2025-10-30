package io.exoquery.annotation


/**
 * Used to annotate a type so that the ExoQuery system knows that it is a value (i.e. a value-XRType)
 * that needs to be encoded/decoded itself and not further broken down into its components during
 * select-query expasion. For example given something like this:
 * ```
 * data class MyDate(val year: Int, val month: Int, val day: Int)
 * data class Customer(name: String, lastOrder: MyDate)
 * sql { Table<Customer>() }
 * // Would be broken down into something like:
 * // SELECT name, lastOrder_year, lastOrder_month, lastOrder_day FROM Customer
 * // However, if we annotate MyDate with ExoValue i.e. data class `Customer(name: String, lastOrder: @ExoValue MyDate)`
 * // then the query will be:
 * // SELECT name, lastOrder FROM Customer
 * ```
 * During deserialization the system will expect to have a serializer dynamcially configured for MyDate (NOT an encoder
 * since ExoValue does not imply the value in encoding is contextual). In order to both mark the property as a ExoQuery value
 * and mark it as Contextual (telling the system to expect a direct decoder for MyValue) annotate the type as @Contextual instead
 * i.e. `data class Customer(name: String, lastOrder: @Contextual MyDate)`
 *
 * Alternatively, it can be specified on the property itself e.g. `data class Customer(name: String, @ExoValue lastOrder: MyDate)`
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class ExoValue


/**
 * Use this to change the name of a column in a query if you do not want to use
 * `@SerialName` from kotlinx.serialization. This is useful if you want to reuse
 * the same generated class serializer for other situations (e.g. JSON serialization).
 * Use it like this:
 * ```
 * data class Person(@ExoField("first_name") val firstName: String, @ExoField("last_name") val lastName: String)
 * // Then when you sql a query like:
 * sql { Table<Person>().filter { it.firstName == "Joe" }
 * // It will come out as:
 * // SELECT first_name, last_name FROM Person WHERE first_name = 'Joe'
 * ```
 * INTERNAL NOTE:
 * Using a Target of AnnotationTarget.FIELD will place this on the backing field and it will need to be retrieved as:
 *  `irCall.symbol.owner.correspondingPropertySymbol?.owner?.backingField?.annotations`
 *  Instead of just:
 *  `irCall .symbol.owner.correspondingPropertySymbol?.owner?.annotations`
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class ExoField(val name: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExoEntity(val name: String)

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
annotation class SqlDynamic

@Deprecated("Renamed to SqlDynamic", ReplaceWith("SqlDynamic"))
typealias CapturedDynamic = SqlDynamic

/**
 * This annotation means that the construct e.g. the SqlQuery represents a value captured during compile-time by the
 * ExoQuery system (via the parser and transformers). It cannot be specified by the user.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class SqlFragment

@Deprecated("Renamed to SqlFragment", ReplaceWith("SqlFragment"))
typealias CapturedFunction = SqlFragment

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class ExoRoomInterface
