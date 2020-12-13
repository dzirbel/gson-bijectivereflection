package com.dzirbel.gson.bijectivereflection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaTest {
    static class TestObject {
        final String stringField;
        final int intField;
        @Nullable
        final String optionalStringField;
        final List<Map<String, Integer>> list;

        TestObject(
                String stringField,
                int intField,
                @Nullable String optionalStringField,
                List<Map<String, Integer>> list
        ) {
            this.stringField = stringField;
            this.intField = intField;
            this.optionalStringField = optionalStringField;
            this.list = list;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return intField == that.intField &&
                    Objects.equals(stringField, that.stringField) &&
                    Objects.equals(optionalStringField, that.optionalStringField) &&
                    Objects.equals(list, that.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stringField, intField, optionalStringField, list);
        }
    }

    private final Gson baseGson = new Gson();

    @ParameterizedTest
    @MethodSource("successCases")
    void testSuccess(TestObject testObject) {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new BijectiveReflectiveTypeAdapterFactory())
                .create();

        final String json = baseGson.toJson(testObject);
        final TestObject out = gson.fromJson(json, TestObject.class);

        assertThat(out).isEqualTo(testObject);
    }

    @ParameterizedTest
    @MethodSource("exceptionCases")
    void testExceptions(TestObject testObject) {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new BijectiveReflectiveTypeAdapterFactory())
                .create();

        final String json = baseGson.toJson(testObject);

        assertThrows(JsonSyntaxException.class, () -> gson.fromJson(json, TestObject.class));
    }

    static List<TestObject> exceptionCases() {
        return Collections.singletonList(new TestObject(null, 0, null, null));
    }

    static List<TestObject> successCases() {
        return Arrays.asList(
                new TestObject(
                        "string value",
                        123,
                        "optional string",
                        Collections.emptyList()
                ),
                new TestObject(
                        "string value",
                        -123,
                        null,
                        Arrays.asList(
                                Map.of("key1", 2),
                                Map.of("key2", 3),
                                Map.of("key3", 5, "key4", 8, "key5", 13)
                        )
                )
        );
    }
}
