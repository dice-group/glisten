package org.dice_group.glisten.core.scorer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class CopaalTest {

    @ParameterizedTest(name = "given \"{0}\" create \"{1}\"")
    @MethodSource("createNamespaces")
    fun `given a set of namespaces, create the correct FILTER`(
        namespaces : List<String>,
        expected : String,
        varName : String
    ){
        val namespacesFilter = NamespaceFilter(namespaces)
        val queryBuilder = StringBuilder()
        namespacesFilter.addFilter(varName, queryBuilder)
        assertEquals(expected, queryBuilder.toString())
    }

    companion object{
        @JvmStatic
        fun createNamespaces(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("http://example.com"), " FILTER(strstarts(str(?var),\"http://example.com\")) \n", "var"),
            Arguments.of(emptyList<String>(), "", "var"),
            Arguments.of(listOf("http://example.com", "http://test.com"),
                " FILTER(strstarts(str(?var),\"http://example.com\") || strstarts(str(?var),\"http://test.com\")) \n",
                "var")
        )
    }
}