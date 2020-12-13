package io.github.dzirbel.gson.bijectivereflection

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Tests that [BijectiveReflectiveTypeAdapterFactory.classes] includes and excludes classes for its type adapters as
 * expected.
 */
internal class ClassExclusionTest {
    @Suppress("unused")
    data class IncludedClass(val id: String)

    @Suppress("unused")
    data class ExcludedClass(val id: String)

    @Suppress("unused")
    data class BaseClass(val included: IncludedClass, val excluded: ExcludedClass)

    private val baseGson = Gson()
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(
            BijectiveReflectiveTypeAdapterFactory(classes = setOf(IncludedClass::class, BaseClass::class))
        )
        .create()

    @Test
    fun testSuccess() {
        // test that ExcludedClass is indeed deserialized by the default adapter; if it were the
        // BijectiveReflectiveTypeAdapterFactory this would throw an exception
        val input = mapOf(
            "included" to mapOf(
                "id" to "1"
            ),
            "excluded" to mapOf()
        )

        val json = baseGson.toJson(input)
        val output = gson.fromJson(json, BaseClass::class.java)
        assertThat(output).isEqualTo(baseGson.fromJson(json, BaseClass::class.java))
    }

    @Test
    fun testException() {
        // test that IncludedClass is still deserialized by the BijectiveReflectiveTypeAdapterFactory and should throw
        // an exception
        val input = mapOf(
            "included" to mapOf<String, String>(),
            "excluded" to mapOf()
        )

        val json = baseGson.toJson(input)
        assertDoesNotThrow { baseGson.fromJson(json, BaseClass::class.java) }
        assertThrows<JsonSyntaxException> { gson.fromJson(json, BaseClass::class.java) }
    }
}
