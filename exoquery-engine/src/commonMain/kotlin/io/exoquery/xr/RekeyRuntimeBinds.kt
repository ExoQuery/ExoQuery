package io.exoquery.xr

import io.exoquery.BID
import io.exoquery.ContainerOfXR

fun <CXR : ContainerOfXR> CXR.rekeyRuntimeBinds(): CXR {
  // Scala:
  //    def rekeyLeafBindsUsing(ast: Ast, bindMap: Map[String, String]) =
  //      Transform(ast) {
  //        case tag: ScalarTag if (bindMap.contains(tag.uid)) => tag.copy(uid = bindMap(tag.uid))
  //      }

  fun rekeyLeafBinds(ast: XR, bindMap: Map<BID, BID>): XR =
    TransformerBuilder().withExpression {
      when (it) {
        is XR.TagForParam -> if (bindMap.contains(it.id)) it.copy(id = bindMap[it.id]!!) else it
        //is XR.TagForSqlExpression -> if (bindMap.contains(it.id)) it.copy(id = bindMap[it.id]!!) else it
        // If there's nothing matching remember to return null so it will recurse into child expression checking for Tags
        // if you don't return null here the transformer will assume transformation for the entire sub-expression is done
        else -> null
      }
    }.invoke(ast)

  //    def rekeyVasesUsing(ast: Ast, bindMap: Map[String, String]) =
  //      Transform(ast) {
  //        case tag: QuotationTag if (bindMap.contains(tag.uid)) => tag.copy(uid = bindMap(tag.uid))
  //      }
  fun rekeyVasesUsing(ast: XR, bindMap: Map<BID, BID>): XR =
    TransformerBuilder()
      .withQuery {
        when (it) {
          is XR.TagForSqlQuery -> if (bindMap.contains(it.id)) it.copy(id = bindMap[it.id]!!) else it
          else -> null
        }
      }.withExpression {
        when (it) {
          is XR.TagForSqlExpression -> if (bindMap.contains(it.id)) it.copy(id = bindMap[it.id]!!) else it
          else -> null
        }
      }.withAction {
        when (it) {
          is XR.TagForSqlAction -> if (bindMap.contains(it.id)) it.copy(id = bindMap[it.id]!!) else it
          else -> null
        }
      }
      .invoke(ast)

  //    def rekeyLeafBinds(quoted: Quoted[_]) = {
  //      val (liftIdMap, newPlanters) = quoted.lifts.map { lift =>
  //        val newId = UUID.randomUUID().toString
  //        val newLift = lift.rekey(newId)
  //        ((lift.uid, newId), newLift)
  //      }.unzip
  //      val newAst = rekeyLeafBindsUsing(quoted.ast, liftIdMap.toMap)
  //      Quoted(newAst, newPlanters, quoted.runtimeQuotes)
  //    }

  fun rekeyLeafBinds(quoted: ContainerOfXR): ContainerOfXR {
    val (liftIdMap, newPlanters) = quoted.params.lifts.map { lift ->
      val newId = BID.new()
      val newLift = lift.withNewBid(newId)
      Pair(lift.id, newId) to newLift
    }.unzip()
    val newAst = rekeyLeafBinds(quoted.xr, liftIdMap.toMap())
    return quoted.rebuild(newAst, quoted.runtimes, quoted.params.copy(lifts = newPlanters))
  }


//    def rekeyRecurse(quoted: Quoted[_]): Quoted[_] = {
//      // rekey leaf binds of the children, for inner most children runtimeQuotes shuold be empty
//      val (vaseIdMap, newVases) = quoted.runtimeQuotes.map { vase =>
//        val newVaseId = UUID.randomUUID().toString
//        // recursively call to rekey the vase (if there are any inner dynamic quotes)
//        val newQuotation = rekeyRecurse(vase.quoted)
//        ((vase.uid, newVaseId), QuotationVase(newQuotation, newVaseId))
//      }.unzip
//      // Then go through the vases themselves (that are on this level and rekey them)
//      val newAst = rekeyVasesUsing(quoted.ast, vaseIdMap.toMap)
//      // finally rekey the leaf-binds of the quotation itself (this should happen on the innermost quotation first
//      // since depth-first recursion is happening here)
//      rekeyLeafBinds(Quoted(newAst, quoted.lifts, newVases))
//    }

  // need to rekey depth-first, otherwise the same uid might be rekeyed multiple times which is not the correct behavior
  // innermost binds need to be rekeyed first
  fun rekeyRecurse(quoted: ContainerOfXR): ContainerOfXR {
    // rekey leaf binds of the children, for inner most children runtimeQuotes shuold be empty
    val (vaseIdMap, newVases) = quoted.runtimes.runtimes.map { (vaseId, vase) ->
      val newVaseId = BID.new()
      // recursively call to rekey the vase (if there are any inner dynamic quotes)
      val newQuotation = rekeyRecurse(vase)
      Pair(vaseId, newVaseId) to Pair(newVaseId, newQuotation)
    }.unzip()
    // Then go through the vases themselves (that are on this level and rekey them)
    val newAst = rekeyVasesUsing(quoted.xr, vaseIdMap.toMap())
    // finally rekey the leaf-binds of the quotation itself (this should happen on the innermost quotation first
    // since depth-first recursion is happening here)
    return rekeyLeafBinds(quoted.rebuild(newAst, quoted.runtimes.copy(runtimes = newVases), quoted.params))
  }

  return rekeyRecurse(this) as CXR
}

// Scala:
//  def dedupeRuntimeBinds: Quoted[T] = {


//    // need to rekey depth-first, otherwise the same uid might be rekeyed multiple times which is not the correct behavior
//    // innermost binds need to be rekeyed first
//    def rekeyRecurse(quoted: Quoted[_]): Quoted[_] = {
//      // rekey leaf binds of the children, for inner most children runtimeQuotes shuold be empty
//      val (vaseIdMap, newVases) = quoted.runtimeQuotes.map { vase =>
//        val newVaseId = UUID.randomUUID().toString
//        // recursively call to rekey the vase (if there are any inner dynamic quotes)
//        val newQuotation = rekeyRecurse(vase.quoted)
//        ((vase.uid, newVaseId), QuotationVase(newQuotation, newVaseId))
//      }.unzip
//      // Then go through the vases themselves (that are on this level and rekey them)
//      val newAst = rekeyVasesUsing(quoted.ast, vaseIdMap.toMap)
//      // finally rekey the leaf-binds of the quotation itself (this should happen on the innermost quotation first
//      // since depth-first recursion is happening here)
//      rekeyLeafBinds(Quoted(newAst, quoted.lifts, newVases))
//    }
//
//    rekeyRecurse(this).asInstanceOf[Quoted[T]]
//  }
