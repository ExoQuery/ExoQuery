package io.exoquery.codegen.model

import io.exoquery.annotation.ExoInternal
import kotlinx.serialization.Serializable as Ser

@Ser
sealed interface LLM {
  @Ser data class Ollama(
    val model: String = DefaultModel,
    val url: String = DefaultUrl
  ) : LLM {
    companion object {
      val DefaultModel = "qwen2.5-coder:0.5b"
      val DefaultUrl = "http://localhost:11434"
    }
  }
  @Ser data class OpenAI(
    val model: String = DefaultModel,
    /**
     * The API key to use for OpenAI requests.
     * DO NOT USE THIS IN PRODUCTION. Instead use `apiKeyEnvVar` to set the API key as an environment variable
     * or use the add `api-key` to your codge-generation properties file (.codegen.properties by default).
     */
    val apiKey: String? = null,
    val apiKeyEnvVar: String? = null,
  ): LLM {
    fun withApiKey(apiKey: String): OpenAI =
      this.copy(apiKey = apiKey)

    companion object {
      val DefaultModel = "gpt-4o-mini"
    }
  }
  @ExoInternal
  companion object {
  }
}
