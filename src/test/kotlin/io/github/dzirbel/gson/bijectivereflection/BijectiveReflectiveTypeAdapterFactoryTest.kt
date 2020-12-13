package io.github.dzirbel.gson.bijectivereflection

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.jvm.jvmName

/**
 * General test for [BijectiveReflectiveTypeAdapterFactory] which tests deserialization.
 */
internal class BijectiveReflectiveTypeAdapterFactoryTest {
    data class Parameters(
        val requireAllClassFieldsUsed: Boolean = true,
        val requireAllJsonFieldsUsed: Boolean = true,
        val allowUnusedNulls: Boolean = true
    ) {
        fun toGson(): Gson {
            return GsonBuilder()
                .registerTypeAdapterFactory(
                    BijectiveReflectiveTypeAdapterFactory(
                        requireAllClassFieldsUsed = requireAllClassFieldsUsed,
                        requireAllJsonFieldsUsed = requireAllJsonFieldsUsed,
                        allowUnusedNulls = allowUnusedNulls,
                    )
                )
                .create()
        }
    }

    data class ExceptionCase(
        val input: Map<String, *>,
        val expectedClass: Class<*>,
        val exceptionType: Class<out Throwable> = JsonSyntaxException::class.java,
        val exceptionMessage: String? = null,
        val parameters: Parameters = Parameters()
    )

    data class SuccessCase(
        val input: Any?,
        val expectedValue: Any? = input,
        val expectedClass: Class<*> = expectedValue?.let { it::class.java } ?: Any::class.java,
        val sameInstance: Boolean = false,
        val parameters: Parameters = Parameters()
    )

    private val baseGson = GsonBuilder().serializeNulls().create()

    data class BadTestObject(
        val field: String,
        @SerializedName("field") val sameField: String
    )

    data class SerializedNameTestObject(
        @SerializedName("field_name") val field: String
    )

    data class OptionalFieldTestObject(
        val requiredField: String,
        @BijectiveReflectiveTypeAdapterFactory.OptionalField val optionalField: String,
        val nullableField: String?
    )

    @ParameterizedTest
    @MethodSource("successCases")
    fun testSuccess(successCase: SuccessCase) {
        val gson = successCase.parameters.toGson()
        val output = gson.fromJson(baseGson.toJson(successCase.input), successCase.expectedClass)
        assertThat(output).isEqualTo(successCase.expectedValue)
        if (successCase.sameInstance) {
            assertThat(output).isSameInstanceAs(successCase.input)
        } else {
            assertThat(output).isNotSameInstanceAs(successCase.input)
        }
    }

    @ParameterizedTest
    @MethodSource("exceptionCases")
    fun testExceptions(exceptionCase: ExceptionCase) {
        val gson = exceptionCase.parameters.toGson()
        val exception = assertThrows(exceptionCase.exceptionType) {
            gson.fromJson(
                baseGson.toJson(exceptionCase.input),
                exceptionCase.expectedClass
            )
        }
        exceptionCase.exceptionMessage?.let { assertThat(exception.message).isEqualTo(it) }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun exceptionCases(): List<ExceptionCase> {
            return listOf(
                ExceptionCase(
                    input = mapOf("stringField" to "abc"),
                    expectedClass = TestObject::class.java,
                    exceptionMessage = "Model class ${TestObject::class.jvmName} required field(s) which were not " +
                        "present in the JSON: [intField]"
                ),
                ExceptionCase(
                    input = mapOf(
                        "stringField" to "abc",
                        "intField" to 123,
                        "unusedField" to "xyz"
                    ),
                    expectedClass = TestObject::class.java,
                    exceptionMessage = "Model class ${TestObject::class.jvmName} does not contain a field for JSON " +
                        "property `unusedField` with value `xyz`"
                ),
                ExceptionCase(
                    input = mapOf(
                        "stringField" to "abc",
                        "intField" to 123,
                        "unusedNull" to null
                    ),
                    expectedClass = TestObject::class.java,
                    parameters = Parameters(allowUnusedNulls = false),
                    exceptionMessage = "Model class ${TestObject::class.jvmName} does not contain a field for JSON " +
                        "property `unusedNull` with value `null`"
                ),
                ExceptionCase(
                    input = mapOf("field" to "value"),
                    expectedClass = BadTestObject::class.java,
                    exceptionType = IllegalArgumentException::class.java,
                    exceptionMessage = "class ${BadTestObject::class.jvmName} declares multiple JSON fields named field"
                ),
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun successCases(): List<SuccessCase> {
            return Fixtures.testObjects
                .plus(SerializedNameTestObject(field = "value"))
                .map { SuccessCase(input = it, sameInstance = it == null || it is Int) }
                .plus(
                    SuccessCase(
                        input = mapOf("field_name" to "value"),
                        expectedValue = SerializedNameTestObject(field = "value")
                    )
                )
                .plus(
                    SuccessCase(
                        input = mapOf("stringField" to "value"),
                        // inflate the expected value from a regular Gson since it can't be made without reflection
                        // (since some fields are uninitialized)
                        expectedValue = Gson().fromJson(
                            Gson().toJson(mapOf("stringField" to "value")),
                            TestObject::class.java
                        ),
                        parameters = Parameters(requireAllClassFieldsUsed = false)
                    )
                )
                .plus(
                    SuccessCase(
                        input = mapOf("requiredField" to "value"),
                        // inflate the expected value from a regular Gson since it can't be made without reflection
                        // (since some fields are uninitialized)
                        expectedValue = Gson().fromJson(
                            Gson().toJson(mapOf("requiredField" to "value")),
                            OptionalFieldTestObject::class.java
                        )
                    )
                )
                .plus(
                    SuccessCase(
                        input = OptionalFieldTestObject(
                            requiredField = "required",
                            optionalField = "optional",
                            nullableField = "nullable"
                        )
                    )
                )
                .plus(
                    SuccessCase(
                        input = mapOf(
                            "stringField" to "abc",
                            "intField" to 123,
                            "unusedField" to "xyz"
                        ),
                        expectedValue = TestObject(stringField = "abc", intField = 123),
                        parameters = Parameters(requireAllJsonFieldsUsed = false)
                    )
                )
                .plus(
                    SuccessCase(
                        input = mapOf(
                            "stringField" to "abc",
                            "intField" to 123,
                            "unusedField" to arrayListOf(1, 2, 3)
                        ),
                        expectedValue = TestObject(stringField = "abc", intField = 123),
                        parameters = Parameters(requireAllJsonFieldsUsed = false)
                    )
                )
                .plus(
                    SuccessCase(
                        input = mapOf(
                            "stringField" to "abc",
                            "intField" to 123,
                            "unusedField" to TestObject(stringField = "string", intField = 2)
                        ),
                        expectedValue = TestObject(stringField = "abc", intField = 123),
                        parameters = Parameters(requireAllJsonFieldsUsed = false)
                    )
                )
                .plus(
                    SuccessCase(
                        input = mapOf(
                            "stringField" to "abc",
                            "intField" to 123,
                            "unusedNull" to null
                        ),
                        expectedValue = TestObject(stringField = "abc", intField = 123)
                    )
                )
        }
    }
}
