package com.dzirbel.gson.bijectivereflection

data class TestObject(
    val stringField: String,
    val intField: Int,
    val nullableStringField: String? = null,
    val nestedObject: TestObject? = null
)

data class TestObjectWithGenerics(
    val stringList: List<String>,
    val testObjects: List<TestObject>
) {
    @Suppress("unused")
    val noBackingField: Int
        get() = stringList.size
}

object Fixtures {
    val testObjects: List<Any?> = listOf(
        null,
        0,
        1,
        -2,
        3.14,
        "",
        "string",
        "1",
        listOf<String>(),
        arrayListOf("a", "b"),
        arrayListOf(2.0, 3.0, 5.0),
        mapOf("key1" to 1.0, "key2" to 2.0),
        TestObject(stringField = "abc", intField = 123),
        TestObject(stringField = "abc", intField = 123, nullableStringField = "xyz"),
        TestObject(
            stringField = "abc",
            intField = 123,
            nestedObject = TestObject(stringField = "nested", intField = 42)
        ),
        TestObjectWithGenerics(
            stringList = listOf("a", "b"),
            testObjects = listOf(
                TestObject(stringField = "abc", intField = 123),
                TestObject(
                    stringField = "abc",
                    intField = 123,
                    nullableStringField = "xyz"
                ),
                TestObject(
                    stringField = "abc",
                    intField = 123,
                    nestedObject = TestObject(
                        stringField = "nested",
                        intField = 42
                    )
                ),
            )
        ),
    )
}
