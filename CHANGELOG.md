# ExoQuery Changelog

### Version 1.7.1.PL (September 28, 2025)
 ```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.20-1.7.1.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.7.1.PL") }
 ```
- **Bugfix**: Fix having + orderBy ([#48](https://github.com/ExoQuery/ExoQuery/pull/48))


### Version 1.7.0.PL (September 28, 2025)
 ```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.20-1.7.0.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.7.0.PL") }
 ```
- **Enhancement**: Implementing HAVING clause ([#47](https://github.com/ExoQuery/ExoQuery/pull/47))

### Version 1.6.1.PL (September 17, 2025)
 ```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.20-1.6.1.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.6.1.PL") }
 ```
- **Infrastructure**: Move to Kotlin 2.2.20 ([#46](https://github.com/ExoQuery/ExoQuery/pull/46))

### Version 1.6.0.PL (September 15, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.6.0.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.6.0.PL") }
 ```
- **Enhancement**: Adding better checks for CapturedDynamic functions ([#44](https://github.com/ExoQuery/ExoQuery/pull/44))
- **Refactor**: Completely get rid of Symbology ([#45](https://github.com/ExoQuery/ExoQuery/pull/45))

### Version 1.5.3.PL (September 12, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.5.3.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.5.3.PL") }
```
- **Bugfix**: Fixing cross-file compile-time queries ([#43](https://github.com/ExoQuery/ExoQuery/pull/43))

### Version 1.5.1.PL (August 28, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.5.1.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.5.1.PL") }
```
- **Bugfix**: Fix code-gen removal of annotations in wrong scenario ([#41](https://github.com/ExoQuery/ExoQuery/pull/41))
- **Enhancement**: Make things with @ExoEntity/@ExoField/@SerialName automatically quoted ([#40](https://github.com/ExoQuery/ExoQuery/pull/40))

### Version 1.5.0.PL (August 25, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.5.0.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.5.0.PL") }
```
- **Enhancement**: Add @Serializeable, continue refining codegen ([#37](https://github.com/ExoQuery/ExoQuery/pull/37))
- **Infrastructure**: Default to using permanent location for record generation ([113ff0d](https://github.com/ExoQuery/ExoQuery/commit/113ff0d))

### Version 1.4.0.PL (August 21, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.4.0.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.4.0.PL") }
```
- **Infrastructure**: Moving to Kotest 6.0.0 ([#36](https://github.com/ExoQuery/ExoQuery/pull/36))
- **Bugfix**: Fixing codegen directory structure creation ([#35](https://github.com/ExoQuery/ExoQuery/pull/35))

### Version 1.4.0.PL.RC5 (August 19, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.4.0.PL.RC5" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.4.0.PL.RC5") }
```
- **Enhancement**: Completing Codegen First Implementation ([#32](https://github.com/ExoQuery/ExoQuery/pull/32))
- **Enhancement**: Additional case for codegen to cover ([#33](https://github.com/ExoQuery/ExoQuery/pull/33))
- **Enhancement**: Fix avg/stddev API ([#34](https://github.com/ExoQuery/ExoQuery/pull/34))

### Version 1.4.0.PL.RC4 (August 14, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.4.0.PL.RC4" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.4.0.PL.RC4") }
```
- **Bugfix**: Fix groupBy(product) and capture{select.filter} cases ([#31](https://github.com/ExoQuery/ExoQuery/pull/31))

### Version 1.4.0.PL.RC3 (August 1, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.4.0.PL.RC3" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.4.0.PL.RC3") }
```
- **Bugfix**: Fix `.count()` type to int ([#30](https://github.com/ExoQuery/ExoQuery/pull/30))

### Version 1.4.0.PL.RC2 (July 6, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.4.0.PL.RC2" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.4.0.PL.RC2") }
```
- **Enhancement**: Allow forward references in simple captures and captured functions ([#21](https://github.com/ExoQuery/ExoQuery/pull/21))
- **Enhancement**: Add interpolated strings parsing ([#25](https://github.com/ExoQuery/ExoQuery/pull/25))
- **Bugfix**: Fixing issue with captured-function in object ([4e5b43f](https://github.com/ExoQuery/ExoQuery/commit/4e5b43f))

### Version 1.4.0.PL.RC1 (June 30, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.2.0-1.4.0.PL.RC1" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.4.0.PL.RC1") }
```
- **Build**: Remove Kotest from non-JVM targets due to Kotlin 2.2.0 plugin compatibility ([#16](https://github.com/ExoQuery/ExoQuery/pull/16))
- **Enhancement**: Prepare for 2.2.0 ([#15](https://github.com/ExoQuery/ExoQuery/pull/15))

### Version 1.3.5.PL-1.3.5 (June 20, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.3.5.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.3.5.PL-1.3.5") }
```

### Version 1.3.2.PL-1.3.2 (May 30, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.3.2.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.3.2.PL-1.3.2") }
```
- **Bugfix**: Fix window Beta Reduction ([#9](https://github.com/ExoQuery/ExoQuery/pull/9))
- **Enhancement**: Improve help message for param serialization errors ([470ab9f](https://github.com/ExoQuery/ExoQuery/commit/470ab9f))

### Version 1.3.1.PL-1.3.1 (May 29, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.3.1.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.3.1.PL-1.3.1") }
```
- **Bugfix**: Fix pretty-builder for query.buildPrettyFor ([#8](https://github.com/ExoQuery/ExoQuery/pull/8))
- **Bugfix**: `Fix param(kotlinx.datetime.*)`. ([#7](https://github.com/ExoQuery/ExoQuery/pull/7))
- **Bugfix**: Fixing various edge-cases for boolean vendorization ([#6](https://github.com/ExoQuery/ExoQuery/pull/6))

### Version 1.3.0.PL-1.3.0 (May 26, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.3.0.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.3.0.PL-1.3.0") }
```
- **Enhancement**: Implement window functions, fix impure + union-flattening cases ([#5](https://github.com/ExoQuery/ExoQuery/pull/5))

### Version 1.2.7.PL-1.2.7 (May 22, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.2.7.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.2.7.PL-1.2.7") }
```
- **Enhancement**: Move to standard JB annotations since it uses KMP ([49ab709](https://github.com/ExoQuery/ExoQuery/commit/49ab709))
- **Enhancement**: Bump controller, make keyword detection case-insensitive ([93bd4ae](https://github.com/ExoQuery/ExoQuery/commit/93bd4ae))
- **Enhancement**: Allow onConflictUpdate/onConflictIgnore to take nullable fields ([4123bb7](https://github.com/ExoQuery/ExoQuery/commit/4123bb7))

### Version 1.2.6.PL-1.2.6 (May 21, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.2.6.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.2.6.PL-1.2.6") }
```
- **Enhancement**: Allow nullable `.excluding(column)` for actions ([ab2ca52](https://github.com/ExoQuery/ExoQuery/commit/ab2ca52))

### Version 1.2.5.PL-1.2.5 (May 21, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.2.5.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.2.5.PL-1.2.5") }
```
- **Testing**: Out-of-box handling of value classes ([11c8fb8](https://github.com/ExoQuery/ExoQuery/commit/11c8fb8))

### Version 1.2.4.PL-1.2.4 (May 19, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.2.4.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.2.4.PL-1.2.4") }
```
- **Bugfix**: Fix setParams with value class members. Bump controller to 3.2.2. ([94029bd](https://github.com/ExoQuery/ExoQuery/commit/94029bd))
- **Enhancement**: Allow value classes for param(...) ([5433a0e](https://github.com/ExoQuery/ExoQuery/commit/5433a0e))
- **Infrastructure**: Beginning to move to toml libs ([eff9914](https://github.com/ExoQuery/ExoQuery/commit/eff9914))
- **Bugfix**: Fix multiple ExprToQuery cases including select-clause query and select 1 ([33db3f6](https://github.com/ExoQuery/ExoQuery/commit/33db3f6))
- **Infrastructure**: Support configuration without ExoOptions i.e. playground env ([5edb48d](https://github.com/ExoQuery/ExoQuery/commit/5edb48d))
- **Bugfix**: Fix SQL synthesis of Kotlin casts ([d982924](https://github.com/ExoQuery/ExoQuery/commit/d982924))
- **Bugfix**: Fix captured functions using extension-position ([993277c](https://github.com/ExoQuery/ExoQuery/commit/993277c))

### Version 1.2.2.PL-1.2.2 (May 9, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.2.2.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.2.2.PL-1.2.2") }
```
- **Bugfix**: Fixing issues with `toString/Int/Long/...` casting ([d4d3e71](https://github.com/ExoQuery/ExoQuery/commit/d4d3e71))
- **Bugfix**: Fix various type-conversion casting, most notably with `!!` ([5d9fc28](https://github.com/ExoQuery/ExoQuery/commit/5d9fc28))
- **Enhancement**: TypeParser should understand @Contextual on Type/Class as XRType.Value ([1c3bb5b](https://github.com/ExoQuery/ExoQuery/commit/1c3bb5b))
- **Bugfix**: Fixing custom serialization with `@Serializable(with = MyClass::class)` annotations on class members ([13b6510](https://github.com/ExoQuery/ExoQuery/commit/13b6510))

### Version 1.2.0.PL-1.2.0 (May 2, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.20-1.2.0.PL" }
dependencies { implementation("io.exoquery:exoquery-runner-jdbc:1.2.0.PL-1.2.0") }
```
- **Enhancement**: Add support for onConflictUpdate and onConflictIgnore in insert queries ([15f978a](https://github.com/ExoQuery/ExoQuery/commit/15f978a))
- **Enhancement**: Set runner-core to be API since it defines things like 'transaction' ([15f978a](https://github.com/ExoQuery/ExoQuery/commit/15f978a))
- **Bugfix**: Fix XRType-parsing errors that occurred in value classes ([15f978a](https://github.com/ExoQuery/ExoQuery/commit/15f978a))
- **Enhancement**: Change onConflict parsing and do not assign default identifier 'x' in the parser ([d906bbc](https://github.com/ExoQuery/ExoQuery/commit/d906bbc))
- **Enhancement**: Add language-injection to free ([d906bbc](https://github.com/ExoQuery/ExoQuery/commit/d906bbc))

### Version 1.1.2.PL-1.1.2 (April 30, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.0-1.1.2.PL" }
dependencies { implementation("io.exoquery:exoquery-jdbc:1.1.2.PL-1.1.2") }
```
- **Bugfix**: Changing all source-main names to be alphanumeric due to DEX issues ([fc60a99](https://github.com/ExoQuery/ExoQuery/commit/fc60a99))
- **Bugfix**: Fix null-value encoder summoning errors ([656a50a](https://github.com/ExoQuery/ExoQuery/commit/656a50a))
- **Enhancement**: Change directory-creation failure (of query-record file) to warn ([cc53a70](https://github.com/ExoQuery/ExoQuery/commit/cc53a70))
- **Infrastructure**: Change `exoquery-runtime` module to `exoquery-engine` ([8908b5b](https://github.com/ExoQuery/ExoQuery/commit/8908b5b))
- **Enhancement**: Only write to query-file if contents different ([c192ad3](https://github.com/ExoQuery/ExoQuery/commit/c192ad3))
- **Infrastructure**: Make controller API dependency since upstreams explicitly need to create it ([0b2935d](https://github.com/ExoQuery/ExoQuery/commit/0b2935d))

### Version 1.1.0.PL-1.1.0 (April 30, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.0-1.1.0.PL" }
dependencies { implementation("io.exoquery:exoquery-jdbc:1.1.0.PL-1.1.0") }
```
- **Enhancement**: Bump to Kotlin 2.2.0 ([0c2ff3a](https://github.com/ExoQuery/ExoQuery/commit/0c2ff3a))

### Version 1.0.1.PL-1.0.1 (April 29, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.0-1.0.1.PL" }
dependencies { implementation("io.exoquery:exoquery-jdbc:1.0.1.PL-1.0.1") }
```
- **Enhancement**: Move runOn(ctr) to common ([934209c](https://github.com/ExoQuery/ExoQuery/commit/934209c))
- **Enhancement**: Refactor SqlQueryApply to use 'of' for correct rendering of top-level free queries ([3ea2319](https://github.com/ExoQuery/ExoQuery/commit/3ea2319))

### Version 1.0.0.PL-1.0.0 (April 29, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.0-1.0.0.PL" }
dependencies { implementation("io.exoquery:exoquery-jdbc:1.0.0.PL-1.0.0") }
```
- **Enhancement**: Introduce `TopLevelFree` so top-level `free` queries do not need additional wrapping ([19a83fe](https://github.com/ExoQuery/ExoQuery/commit/19a83fe))

### Version 0.2.0.PL-0.2.0 (April 24, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.0-0.2.0.PL" }
dependencies { implementation("io.exoquery:exoquery-jdbc:0.2.0.PL-0.2.0") }
```
- **Enhancement**: Implement naming override annotations ([edeb3a6](https://github.com/ExoQuery/ExoQuery/commit/edeb3a6))

### Version 0.1.0.PL-0.1.0 (April 22, 2025)
```kotlin
plugins { id("io.exoquery.exoquery-plugin") version "2.1.0-0.1.0.PL" }
dependencies { implementation("io.exoquery:exoquery-jdbc:0.1.0.PL-0.1.0") }
```
- **Infrastructure**: First release version ([f12a849](https://github.com/ExoQuery/ExoQuery/commit/f12a849))
