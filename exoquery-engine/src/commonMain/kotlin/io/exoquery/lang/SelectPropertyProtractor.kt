package io.exoquery.lang

import io.decomat.*
import io.exoquery.xr.XRType
import io.exoquery.xr.XR
import io.exoquery.xr.XR.Ident
import io.exoquery.xr.XR.Visibility.Visible
import io.exoquery.xr.XR.Visibility.Hidden
import io.exoquery.xr.get


/**
 * Simple utility that checks if if an AST entity refers to a entity. It
 * traverses through the context types to find out what kind of context a
 * variable refers to. For example, in a query like:
 * {{{query[Pair].map(p -> Pair(p.a, p.b)).distinct.map(p -> (p.b, p.a))}}}
 *
 * Yielding SQL like this:
 * {{{SELECT p.b, p.a FROM (SELECT DISTINCT p.a, p.b FROM pair p) AS p}}}
 *
 * the inner p.a and p.b will have a TableContext while the outer p.b and p.a
 * will have a QueryContext.
 *
 * Note that there are some cases where the id of a field is not in a
 * FromContext at all. For example, in a query like this:
 * {{{query[Human].filter(h -> query[Robot].filter(r -> r.friend == h.id).nonEmpty)}}}
 *
 * Where the sql produced is something like this:
 * {{{SELECT h.id FROM human h WHERE EXISTS (SELECT r.* FROM robot r WHERE r.friend = h.id)}}}
 *
 * the field `r.friend`` is selected from a sub-query of an SQL operation (i.e.
 * `EXISTS (...)`) so a from-context of it will not exist at all. When deciding
 * which properties to treat as sub-select properties (e.g. if we want to make
 * sure NOT to apply a naming-schema on them) we need to take care to understand
 * that we may not know the FromContext that a property comes from since it may
 * not exist.
 */
data class InContext(val from: List<FromContext>) {
  // Are we sure it is a subselect
  fun isSubselect(ast: XR) =
    when (contextReferenceType(ast)) {
      is InQueryContext -> true
      else -> false
    }

  //
  // Are we sure it is a table reference
  fun isEntityReference(ast: XR) =
    when (contextReferenceType(ast)) {
      is InTableContext -> true
      is InInfixContext -> true
      else -> false // i.e. the null case and other cases
    }

  fun contextReferenceType(ast: XR): InContextType? = run {
    val references = collectTableAliases(from)
    return on(ast).match(
      case(Ident[Is()])
        .then { name -> references.get(name) },
      case(PropertyMatryoshka[Ident[Is()], Is()])
        .then { (name), _ -> references.get(name) }
    )
  }

  //
  private fun collectTableAliases(contexts: List<FromContext>): Map<String, InContextType> =
    contexts.map {
      when (it) {
        is TableContext -> mapOf(it.alias to InTableContext)
        is QueryContext -> mapOf(it.alias to InQueryContext)
        is ExpressionContext -> mapOf(it.alias to InInfixContext)
        is FlatJoinContext -> collectTableAliases(listOf(it.from))
      }
    }.fold(mapOf<String, InContextType>(), { a, b -> a + b })


  sealed interface InContextType
  object InTableContext : InContextType
  object InQueryContext : InContextType
  object InInfixContext : InContextType

}


data class SelectPropertyProtractor(val from: List<FromContext>) {
  val inContext = InContext(from)

  private fun nonAbstractQuat(from: XRType, or: XRType?) =
    when {
      from.isAbstract() && or != null && or.nonAbstract() -> or
      else -> from
    }

  /**
   * Turn product quats into case class asts e.g. XRType.Product(name:V,age:V) =>
   * CaseClass(name->p.name,age->p.age) `alternateQuat` is there in case it's a
   * top-level expansion and the identifier is generic or unknown (or an
   * abstract product quat) so we want to try and get information from the quat
   * from the [T] of the executeQuery[T] itself (similar to quatOf[T]) this
   * information is already supplied by macro expansion constructs of the query execution pipeline.
   */
  operator fun invoke(ast: XR.Expression, alternateQuat: XRType?): List<Pair<XR.Expression, List<String>>> =
    on(ast).match(
      case(Core()).then { id ->
        // The quat is considered to be an entity if it is either:
        // a) Found in the table references (i.e. it's an actual table in the subselect) or...
        // b) We are selecting fields from an infix e.g. `sql"selectPerson()".as[Query[Person]]`
        val isEntity = inContext.isEntityReference(id)
        val effectiveQuat = nonAbstractQuat(id.type, alternateQuat)

        when (effectiveQuat) {
          is XRType.Product -> ProtractQuat(isEntity)(effectiveQuat, id)
          else -> listOf(id to listOf())
        }
      },
      // Assuming a property contains only an Ident, Free or Constant at this point
      // and all situations where there is a case-class, tuple, etc... inside have already been beta-reduced
      case(PropertyMatryoshka[Core(), Is()]).thenThis { id, _ ->
        val prop = this
        val isEntity = inContext.isEntityReference(id)
        val effectiveQuat = nonAbstractQuat(prop.type, alternateQuat)

        when (effectiveQuat) {
          is XRType.Product -> ProtractQuat(isEntity)(effectiveQuat, prop)
          else -> listOf(prop to listOf())
        }
      }
    ) ?: listOf(ast to emptyList())


}

/*
 * Protract Nested Product XRTypes into a list of paths to each one.
 * Take a quat and project it out as nested properties with some core XR inside.
 * quat: CC(foo,bar:Type.CC(a,b)) with core id:Ident(x) ->
 *   List( Prop(id,foo) [foo], Prop(Prop(id,bar),a) [bar.a], Prop(Prop(id,bar),b) [bar.b] )
 */
data class ProtractQuat(val refersToEntity: Boolean) {
  operator fun invoke(quat: XRType.Product, core: XR.Expression): List<Pair<XR.Property, List<String>>> =
    applyInner(quat, core)

  fun applyInner(quat: XRType.Product, core: XR.Expression): List<Pair<XR.Property, List<String>>> {
    // Property (and alias path) should be visible unless we are referring directly to a TableContext
    // with an Entity that has embedded fields. In that case, only top levels should show since
    // we're selecting from an actual table and in that case, the embedded paths don't actually exist.
    val wholePathVisible = !refersToEntity

    return quat.fields.flatMap { (name, child) ->
      when (child) {
        is XRType.Product -> {
          // Should not need this
          // val fieldName = quat.renames.find(_._1 == name).map(_._2).getOrElse(name)

          applyInner(
            child,
            XR.Property(
              core,
              // If the quat is renamed, create a property representing the renamed field, otherwise use the quat field for the property
              name,
              /*
              Is the parent-XRType represents an product (i.e. an entity in this case) and the field of it has a rename (if it has a @ExoField/@SerialName annotation)
              e.g:
              data class Person(name: Name, ...)
              data class Name(@SerialName("first_name") firstName: String, ...)
              Then the Product types will be something like:
              Person: CC(name:CC(first_name:String /*hasRename=true*/,...),..., meta=Meta(fieldsWithRename=Set("first_name")))

              then we need to make sure it it is set to has-rename so that the SqlIdiom tokenizer will know to quote it
              (note that the name of the field itself is already the renamed version, so we don't need to do anything special with it there)

              Interestingly, the `name` field itself could be annotated with @SerialName too, but it would be marked Visibility.Hidden as an intermediate property
             */
              XR.HasRename.hasOrNot(refersToEntity && child.meta.fieldsWithRename.contains(name)),
              /* If the property represents a property of a Entity (i.e. we're selecting from an actual table,
             * then the entire projection of the XRType should be visible (since subsequent aliases will
             * be using the entire path.
             * Take: Bim(bid:Int, mam:Mam), Mam(mid:Int, mood:Int)
             * Here is an example:
             * SELECT g.mam FROM
             *    SELECT gim.bim: CC(bid:Int,mam:CC(mid:Int,mood:Int)) FROM g
             *
             * This needs to be projected into:
             * SELECT g.mammid, g.mammood FROM                         -- (2) so their selection of sub-properties from here is correct
             *    SELECT gim.mid AS mammid, gim.mood as mammood FROM g -- (1) for mammid and mammood need full quat path here...
             *
             * (See examples of this in ExpandNestedQueries multiple embedding levels series of tests. Also note that since sub-selection
             * is typically done from tuples, paths typically start with _1,_2 etc...)
             */
              if (wholePathVisible) Visible else Hidden,
              XR.Location.Synth
            )
          ).map { (prop, path) ->
            (prop to listOf(name) + path)
          }
        }

        else ->
          // If the quat is renamed, create a property representing the renamed field, otherwise use the quat field for the property
          // val fieldName = quat.renames.find(_._1 == name).map(_._2).getOrElse(name)
          // The innermost entity of the quat. This is always visible since it is the actual column of the table
          listOf((
              XR.Property(
                core,
                name,
                XR.HasRename.hasOrNot(refersToEntity && quat.meta.fieldsWithRename.contains(name)),
                Visible,
                XR.Location.Synth
              ) to listOf(name)))
      }.toList()
    }
  }
}
