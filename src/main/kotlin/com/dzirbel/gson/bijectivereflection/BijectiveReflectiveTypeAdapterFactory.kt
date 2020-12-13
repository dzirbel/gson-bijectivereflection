package com.dzirbel.gson.bijectivereflection

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.ConstructorConstructor
import com.google.gson.internal.`$Gson$Types`
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * A [Gson] [TypeAdapterFactory] which overrides the default [ReflectiveTypeAdapterFactory] to provides stricter type
 * adapters for user-defined classes, requiring that all JSON elements are reflected in the class and vice versa.
 *
 * Like the default [ReflectiveTypeAdapterFactory], this [TypeAdapterFactory] deserializes JSON elements by reflecting
 * over the fields of the destination class. Unlike the default [ReflectiveTypeAdapterFactory], [TypeAdapter]s provided
 * by this factory require that the input JSON and the destination class have one-to-one fields (unless the JSON field
 * has a null value). Serialization (writing classes to JSON) is delegated to the [ReflectiveTypeAdapterFactory].
 */
class BijectiveReflectiveTypeAdapterFactory(
    /**
     * Requires that every member field of a destination class is required to be present in the JSON; default true.
     *
     * Nullable fields (either Kotlin nullable or annotated with either [javax.annotation.Nullable] or
     * [org.jetbrains.annotations.Nullable]) or fields annotated with [OptionalField] are omitted.
     */
    private val requireAllClassFieldsUsed: Boolean = true,

    /**
     * Requires that for every JSON element there must be a corresponding field in the destination class; default true.
     *
     * If [allowUnusedNulls] is true, then if a JSON element has null value, it is not required to have an associated
     * class field.
     */
    private val requireAllJsonFieldsUsed: Boolean = true,

    /**
     * Allows the input JSON to contain fields with a null value for which there is no corresponding field in the
     * destination class.
     */
    private val allowUnusedNulls: Boolean = true,

    /**
     * An optional set of [KClass]es which this [TypeAdapterFactory] will deserialize into; when null (the default), it
     * deserializes into all classes which the the default [ReflectiveTypeAdapterFactory] handles.
     */
    private val classes: Set<KClass<*>>? = null
) : TypeAdapterFactory {

    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class OptionalField

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (classes != null && !classes.contains(type.rawType.kotlin)) {
            return null
        }

        val delegate = gson.getDelegateAdapter(this, type)
        return (delegate as? ReflectiveTypeAdapterFactory.Adapter)
            ?.let {
                BijectiveReflectiveTypeAdapter(
                    gson = gson,
                    type = type,
                    delegate = it,
                    requireAllJsonFieldsUsed = requireAllJsonFieldsUsed,
                    requireAllClassFieldsUsed = requireAllClassFieldsUsed,
                    allowUnusedNulls = allowUnusedNulls
                )
            }
    }
}

/**
 * A [TypeAdapter] which can [read] JSON objects into instances of [T].
 */
private class BijectiveReflectiveTypeAdapter<T>(
    private val gson: Gson,
    private val type: TypeToken<T>,
    private val delegate: ReflectiveTypeAdapterFactory.Adapter<T>,
    private val requireAllJsonFieldsUsed: Boolean,
    private val requireAllClassFieldsUsed: Boolean,
    private val allowUnusedNulls: Boolean
) : TypeAdapter<T>() {
    /**
     * The member fields of [T]; paired by [KProperty1] (Kotlin reflection) and [Field] (Java reflection); only
     * including fields which have a non-null [Field], i.e. those which have a backing field.
     */
    private val fields: List<Pair<KProperty1<in T, *>, Field>> by lazy {
        type.rawType.kotlin.memberProperties.mapNotNull { kProperty ->
            kProperty.javaField?.let { javaField -> Pair(kProperty, javaField) }
        }
    }

    /**
     * The member fields of [T] which are required when [requireAllClassFieldsUsed] is true, i.e. the subset of [fields]
     * which are non-nullable and not annotated with [BijectiveReflectiveTypeAdapterFactory.OptionalField].
     */
    private val requiredFields: List<Field> by lazy {
        fields
            .filter { (kProperty, _) ->
                !kProperty.returnType.isMarkedNullable &&
                    !kProperty.hasAnnotation<javax.annotation.Nullable>() &&
                    !kProperty.hasAnnotation<org.jetbrains.annotations.Nullable>() &&
                    !kProperty.hasAnnotation<BijectiveReflectiveTypeAdapterFactory.OptionalField>()
            }
            .map { it.second }
    }

    private val constructor by lazy { ConstructorConstructor(emptyMap()).get(type) }

    /**
     * Returns a [Set] of the serialized names for this class [Field]; JSON fields which match any of these names should
     * be serialized into this [Field].
     */
    private val Field.serializedNames: Set<String>
        get() {
            return getAnnotation(SerializedName::class.java)?.let { annotation ->
                setOfNotNull(annotation.value).plus(annotation.alternate)
            } ?: setOf(gson.fieldNamingStrategy().translateName(this))
        }

    /**
     * Determines the [Field] which should be set for the JSON field of the given [name].
     */
    private fun fieldFor(name: String): Field? {
        return fields.find { (_, javaField) -> javaField.serializedNames.contains(name) }?.second
    }

    override fun write(out: JsonWriter, value: T) {
        delegate.write(out, value)
    }

    override fun read(reader: JsonReader): T? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val instance = constructor.construct()
        val missingFields = if (requireAllClassFieldsUsed) requiredFields.toMutableSet() else null

        try {
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                val field = fieldFor(name)
                    ?: if (requireAllJsonFieldsUsed && (allowUnusedNulls || reader.peek() != JsonToken.NULL)) {
                        val jsonValue = gson.getAdapter(Any::class.java).read(reader)
                        throw JsonSyntaxException(
                            "Model class ${type.type.typeName} does not contain a field for JSON property " +
                                "`$name` with value `$jsonValue`"
                        )
                    } else {
                        // skip past fields on JSON which have no class field when requireAllJsonFieldsUsed is false (or
                        // when allowUnusedNulls is true and the value is null)

                        // in the null case, read in the next null element
                        if (!requireAllClassFieldsUsed) {
                            reader.nextNull()
                        }

                        continue
                    }

                if (!field.trySetAccessible()) {
                    throw JsonSyntaxException("Could not make $field accessible")
                }

                val fieldType = `$Gson$Types`.resolve(type.type, type.rawType, field.genericType)
                val adapter = gson.getAdapter(TypeToken.get(fieldType))
                val fieldValue = adapter.read(reader)

                field[instance] = fieldValue
                missingFields?.remove(field)
            }
        } catch (e: IllegalStateException) {
            throw JsonSyntaxException(e)
        }
        reader.endObject()

        if (missingFields?.isNotEmpty() == true) {
            throw JsonSyntaxException(
                "Model class ${type.type.typeName} required field(s) which were not present in the JSON: " +
                    missingFields.joinToString { it.serializedNames.toString() }
            )
        }

        return instance
    }
}
