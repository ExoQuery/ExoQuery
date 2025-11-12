package io.exoquery.plugin.trees

import io.exoquery.plugin.classIdOf
import org.jetbrains.kotlin.name.ClassId

// TODO need to introduce CallType to know what is pure and what is not. Also need to ahve equivalent data-structure for global calls
object MethodWhitelist {
  data class HostMethods(val host: ClassId, val allowedMethods: List<AllowedMethod>)
  data class AllowedMethod(val name: String, val expectedNumArgs: Int? = null)

  private inline fun <reified T> classIdOrFail(label: String) = classIdOf<T>() ?: throw IllegalArgumentException("Cannot create MethodWhitelist. Cannot get classId of: ${label}")

  private val data: List<HostMethods> =
    listOf(
      HostMethods(classIdOrFail<String>("String"),
        listOf(
          AllowedMethod("contains"),
          AllowedMethod("substring"),
          AllowedMethod("uppercase"),
          AllowedMethod("lowercase"),
          AllowedMethod("length"),
          AllowedMethod("trim", 1),
        )
      )
    )

  private val dataMap = data.map { it.host to it.allowedMethods }.toMap()

  private val allHosts = data.map { it.host }

  fun allowedHost(host: ClassId) = data.find { it.host == host } != null
  fun allowedMethodForHost(host: ClassId?, method: String?, numArgs: Int): Boolean =
    if (host == null || method == null)
      false
    else
      dataMap[host]?.let {
        // We have an entry for the particular host-type and the expected number of args matches the actual args of the call
        it.any {
          it.name == method && (it.expectedNumArgs == null || it.expectedNumArgs == numArgs)
        }
      } ?: false
}
