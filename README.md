# Gson Bijective Reflection

![Build](https://github.com/dzirbel/gson-bijectivereflection/workflows/Build/badge.svg)
[![codecov](https://codecov.io/gh/dzirbel/gson-bijectivereflection/branch/main/graph/badge.svg?token=HEXBHVPVXN)](https://codecov.io/gh/dzirbel/gson-bijectivereflection)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dzirbel/gson-bijectivereflection)](https://search.maven.org/artifact/io.github.dzirbel/gson-bijectivereflection)

`gson-bijectivereflection` is an extension to [Gson](https://github.com/google/gson) for Kotlin and
Java which adds stricter deserialization of user-provided classes.

It adds a `BijectiveReflectiveTypeAdapterFactory` which requires that JSON deserialization of
classes is _bijective_:
- all non-nullable fields in the destination class must be present in the JSON (surjective)
- all JSON elements must be mapped to fields on the destination class; no JSON elements can be
  unused (injective, kind of)

(`BijectiveReflectiveTypeAdapterFactory` is customizable, including to require only surjectivity
or injectivity)

This library can be used to make your JSON deserialization more strict, in order to verify that your
class models closely reflect the input JSON. It is particularly useful for testing; for example, if
you inject your `Gson` object, you can configure only your tests to use the
`BijectiveReflectiveTypeAdapterFactory` and your production code to be more lenient.

WARNING: While `gson-bijectivereflection` is a small library, it depends on
[Kotlin reflection](https://kotlinlang.org/docs/reference/reflection.html), which is a large (~3 MB)
[artifact](https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect). This may
restrict its usage in space-constrained contexts, such as Android apps.

## Download

Gradle (Groovy DSL):

```
dependencies {
    implementation 'io.github.dzirbel.gson-bijectivereflection:1.0.0'
}
```

Gradle (Kotlin DSL):

```
dependencies {
    implementation("io.github.dzirbel.gson-bijectivereflection:1.0.0")
}
```

Maven:

```
<dependency>
    <groupId>io.github.dzirbel</groupId>
    <artifactId>gson-bijectivereflection</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

To apply the `BijectiveReflectiveTypeAdapterFactory`, supply it when building your `Gson` object:

```
import com.dzirbel.gson.bijectivereflection.BijectiveReflectiveTypeAdapterFactory

val gson = GsonBuilder()
    .registerTypeAdapterFactory(BijectiveReflectiveTypeAdapterFactory())
    .create()
```

That's it! The `BijectiveReflectiveTypeAdapterFactory` will be used to deserialize JSON into classes
for this `gson`, and can be configured further via constructor parameters. It will throw a
`JsonSyntaxException` when deserializing (i.e. `gson.fromJson(...)`) JSON that doesn't meet the
bijective criteria. See the class documentation for more details.

## Examples

```
// Class:
class Example1(val stringField: String, val intField: Int)

// JSON:
{ stringField: "string value", intField: 123 }
```

IS bijective.

---

```
// Class:
class Example2(val stringField: String, val intField: Int, val nullableStringField: String?)

// JSON:
{ stringField: "string value", intField: 123 }
```

IS still bijective, since `nullableStringField` has a nullable type `String?`.

---

```
// Class:
class Example3(val stringField: String, val intField: Int)

// JSON:
{ stringField: "string value", intField: 123, anotherField: ["another", "value"] }
```

IS NOT bijective, since `anotherField` does not have a corresponding field in `Example3`.

---

```
// Class:
class Example4(val stringField: String, val intField: Int)

// JSON:
{ stringField: "string value" }
```

IS NOT bijective, since `Example4.intField` does not have a value in the JSON. By default `Gson`
would leave `intField` as `null`, which can lead to unexpected `NullPointerException`s in Kotlin,
since it is of the non-nullable type `Int`.

---

```
// Java class:
class Example5 {
    String stringField;
    @Nullable nullableString;
}

// JSON:
{ stringField: "string value" }
```

IS bijective, since `@Nullable` can be used like Kotlin nullability to denote that the field is
not required in the JSON.

## License

`gson-bijectivereflection` is provided under the [MIT License](LICENSE).
