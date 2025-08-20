@file:OptIn(ExoInternal::class)

package io.exoquery.generation.typemap

import io.exoquery.annotation.ExoInternal
import kotlinx.serialization.Serializable as Ser

@Ser
data class TypeMap private constructor (val entries: List<Pair<From, ClassOf>>) {
  companion object {
    /**
     * Create the typemap using this API like this:
     * ```
     * TypeMap(
     *   From("users", "email") to ClassOf<String>(),
     *   From("users", "id") to ClassOf("kotlin.Int"),
     * )
     * ```
     *
     * (NOTE: Unlifting does not even consider the constructor)
     */
    operator fun invoke(vararg entries: Pair<From, ClassOf>): TypeMap = TypeMap(entries.toList())
  }
}

@Ser data class From constructor(
  /**
   * Do you want to map a column to a given type only if it comes from a particular column?
   * If so use this Regex for the column name, e.g. "email" or "id".
   */
  val column: String? = null,

  /**
   * Do you wan to map a column to a given type only if it comes from a particular table?
   * If so use this Regex for the table name, e.g. "users"
   */
  val table: String? = null,

  /**
   * Do you want to map a column to a given type only if it comes from a particular schema?
   * If so, use this Regex for the schema name, e.g. "myschema".
   */
  val schema: String? = null,

  /**
   * Do you want to map a column to a given type only if it matches the name of a specify type (in the TYPE_NAME JDBC Metadata)?
   * If so, use this Regex for the type name, e.g. "VARCHAR" (i.e. java.sql.Types.VARCHAR).
   */
  val typeName: String? = null,

  /**
   * Do you want to map a column to a given type only if it matches the JDBC data type number?
   * If so, use this number, e.g. 12 for VARCHAR.
   */
  val typeNum: Int? = null,

  /** If true, the table and column names are matched (by regex) case-sensitively */
  val matchCaseSensitive: Boolean = DefaultMatchCaseSensitive
) {
  companion object {
    @ExoInternal val DefaultMatchCaseSensitive = false
  }
}

@Ser data class ClassOf(val fullTypePath: String) {
  companion object {
    inline operator fun <reified T> invoke(): ClassOf = ClassOf(T::class.qualifiedName ?: T::class.simpleName ?: "UnknownType")
  }
}
