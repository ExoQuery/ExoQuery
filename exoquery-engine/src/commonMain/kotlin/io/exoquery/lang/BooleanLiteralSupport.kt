package io.exoquery.lang

import io.exoquery.xr.BetaReduction
import io.exoquery.xr.XR
import io.exoquery.xr.XRType

interface BooleanLiteralSupport : SqlIdiom {
  override fun normalizeQuery(xr: XR.Query): XR.Query = run {
    val norm = super.normalizeQuery(xr)
    val vendorized = VendorizeBooleans(norm) //BetaReduction(VendorizeBooleans(norm))
    vendorized.asQuery()
  }

  override fun xrConstTokenImpl(constImpl: XR.Const): Token = with(constImpl) {
    when {
      // By types we know here that it's always XRType.BooleanValue
      this is XR.Const.Boolean ->
        StringToken(if (value) "1" else "0")
      else ->
        super.xrConstTokenImpl(constImpl)
    }
  }
}

//trait BooleanLiteralSupport extends SqlIdiom {
//
//  override def normalizeAst(
//    ast: Ast,
//    concatBehavior: ConcatBehavior,
//    equalityBehavior: EqualityBehavior,
//    idiomContext: IdiomContext
//  ) = {
//    val norm = SqlNormalize(ast, idiomContext.config, makeCache(), concatBehavior, equalityBehavior)
//    if (Messages.smartBooleans)
//      VendorizeBooleans(norm)
//    else
//      norm
//  }
//
//  override implicit def valueTokenizer(implicit
//    astTokenizer: Tokenizer[Ast],
//    strategy: NamingStrategy
//  ): Tokenizer[Value] =
//    Tokenizer[Value] {
//      case Constant(b: Boolean, Quat.BooleanValue) =>
//        StringToken(if (b) "1" else "0")
//      case Constant(b: Boolean, Quat.BooleanExpression) =>
//        StringToken(if (b) "1 = 1" else "1 = 0")
//      case other =>
//        super.valueTokenizer.token(other)
//    }
//}
