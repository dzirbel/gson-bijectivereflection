package io.github.dzirbel.gson.bijectivereflection

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests that the [BijectiveReflectiveTypeAdapterFactory] does affect Gson serialization.
 */
internal class WriteTest {
    @ParameterizedTest
    @MethodSource("testObjects")
    fun testPlain(testObject: Any?) {
        val baseGson = Gson()
        val gson = GsonBuilder().registerTypeAdapterFactory(BijectiveReflectiveTypeAdapterFactory()).create()

        assertThat(gson.toJson(testObject)).isEqualTo(baseGson.toJson(testObject))
    }

    @ParameterizedTest
    @MethodSource("testObjects")
    fun testPrettyPrinting(testObject: Any?) {
        val baseGson = GsonBuilder().setPrettyPrinting().create()
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(BijectiveReflectiveTypeAdapterFactory())
            .create()

        assertThat(gson.toJson(testObject)).isEqualTo(baseGson.toJson(testObject))
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun testObjects() = Fixtures.testObjects
    }
}
