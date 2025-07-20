package io.exoquery.codegen.model


data class AssemblingStrategy(
  val schemaGroupingStrategy: SchemaGroupingStrategy,
  val codeWrapperType: CodeWrapperType,
  val fileNamingStrategy: FileNamingStrategy
) {
  companion object {
    val SchemaPerPackage: AssemblingStrategy =
      AssemblingStrategy(
        SchemaGroupingStrategy.DoNotGroup,
        CodeWrapperType.PackageGen,
        ByTable
      )

    val SchemaPerObject: AssemblingStrategy =
      AssemblingStrategy(
        SchemaGroupingStrategy.GroupBySchema,
        CodeWrapperType.ObjectGen,
        BySchema
      )
  }
}

sealed interface SchemaGroupingStrategy {
  data object GroupBySchema: SchemaGroupingStrategy
  data object DoNotGroup: SchemaGroupingStrategy
}


// Scala:
//object PackagingStrategy {
//
//  object ByPackageObject {
//    private def packageByNamespace(prefix: String) = PackageObjectByNamespace(prefix, _.table.namespace)
//
//    def Simple(packagePrefix: String = "") =
//      PackagingStrategy(
//        GroupByPackage,
//        packageByNamespace(packagePrefix),
//        packageByNamespace(packagePrefix),
//        ByPackageObjectStandardName
//      )
//  }
//
//  object ByPackageHeader {
//    private def packageByNamespace(prefix: String) = PackageHeaderByNamespace(prefix, _.table.namespace)
//
//    /**
//     * Use this strategy when you want a separate source code file (or string)
//     * for every single table. Typically you'll want to use this when table
//     * schemas are very large and you want to minimize the footprint of your
//     * imports (i.e. since each file is a separate table you can be sure to just
//     * imports the exact tables needed for every source file).
//     */
//    def TablePerFile(packagePrefix: String = "") =
//      PackagingStrategy(
//        DoNotGroup,
//        packageByNamespace(packagePrefix),
//        packageByNamespace(packagePrefix),
//        ByTable
//      )
//
//    /**
//     * When you want each file (or string) to contain an entire schema, use this
//     * strategy. This is useful for code-generators that have common code
//     * per-schema for example the ComposeableTraitsGen that creates Traits
//     * representing database schemas that can be composed with Contexts.
//     */
//    def TablePerSchema(packagePrefix: String = "") =
//      PackagingStrategy(
//        GroupByPackage,
//        packageByNamespace(packagePrefix),
//        packageByNamespace(packagePrefix),
//        ByPackageName
//      )
//  }
//
//  def NoPackageCombined =
//    PackagingStrategy(
//      GroupByPackage,
//      NoPackage,
//      NoPackage,
//      ByPackageName
//    )
//
//  def NoPackageSeparate =
//    PackagingStrategy(
//      DoNotGroup,
//      NoPackage,
//      NoPackage,
//      ByPackageName
//    )
//}
//
//case class PackagingStrategy(
//  packageGroupingStrategy: PackageGroupingStrategy,
//  packageNamingStrategyForCaseClasses: PackageNamingStrategy,
//  packageNamingStrategyForQuerySchemas: PackageNamingStrategy,
//  fileNamingStrategy: FileNamingStrategy
//)


// Scala:
//case class PackageHeaderByNamespace(val prefix: String, val namespaceMaker: PackageNamingStrategy.NamespaceMaker)
//extends PackageNamingStrategy
//with ByName {
//  override def apply(table: TablePrepared[_, _]): CodeWrapper = PackageHeader(byName(table))
//}
//case class PackageObjectByNamespace(val prefix: String, val namespaceMaker: PackageNamingStrategy.NamespaceMaker)
//extends PackageNamingStrategy
//with ByName {
//  override def apply(table: TablePrepared[_, _]): CodeWrapper = PackageObject(byName(table))
//}
//case class SimpleObjectByNamespace(val prefix: String, val namespaceMaker: PackageNamingStrategy.NamespaceMaker)
//extends PackageNamingStrategy
//with ByName {
//  override def apply(table: TablePrepared[_, _]): CodeWrapper = SimpleObject(byName(table))
//}
//case object NoPackage extends PackageNamingStrategy {
//  override def apply(table: TablePrepared[_, _]): CodeWrapper = NoWrapper
//}
