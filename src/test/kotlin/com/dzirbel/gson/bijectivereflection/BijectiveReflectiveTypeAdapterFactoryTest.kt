package com.dzirbel.gson.bijectivereflection

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.jvm.jvmName

// TODO test all parameter combinations
// TODO test SerializedName
// TODO test OptionalField
// TODO add more complicated test cases (Maps, large classes, etc)
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

    private val baseGson = Gson()

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
        val exception = assertThrows<JsonParseException> {
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
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun successCases(): List<SuccessCase> {
            return Fixtures.testObjects.map { SuccessCase(input = it, sameInstance = it == null || it is Int) }
        }
    }
}
