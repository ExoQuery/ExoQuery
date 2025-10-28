package io.exoquery.lang

interface StatelessTokenTransformer {
  fun invoke(token: Token): Token =
    when (token) {
      is ParamMultiToken -> invoke(token)
      is ParamMultiTokenRealized -> invoke(token)
      is ParamSingleToken -> invoke(token)
      is ParamSingleTokenRealized -> invoke(token)
      is ParamBatchToken -> invoke(token)
      is ParamBatchTokenRealized -> invoke(token)
      is SetContainsToken -> invoke(token)
      is Statement -> invoke(token)
      is StringToken -> invoke(token)
      is TokenContext -> invoke(token)
    }

  fun invoke(token: StringToken): Token = token
  fun invoke(token: ParamMultiToken): Token = token
  fun invoke(token: ParamMultiTokenRealized): Token = token
  fun invoke(token: ParamSingleToken): Token = token
  fun invoke(token: ParamSingleTokenRealized): Token = token
  fun invoke(token: ParamBatchToken): Token = token
  fun invoke(token: ParamBatchTokenRealized): Token = token
  fun invoke(token: TokenContext): Token = TokenContext(invoke(token.content), token.kind)

  fun invoke(token: SetContainsToken): Token =
    SetContainsToken(
      invoke(token.a),
      invoke(token.op),
      invoke(token.b)
    )

  fun invoke(token: Statement): Token =
    Statement(token.tokens.map { invoke(it) })

  companion object {
    operator fun invoke(f: (Token) -> Token?) =
      object : StatelessTokenTransformer {
        override fun invoke(token: Token): Token =
          f(token) ?: super.invoke(token)
      }
  }
}
