package org.dice_group.glisten.core.utils

import org.apache.commons.io.FileUtils
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RDFUtilsTest {


    @Test
    fun `remove literals from model`() {
        val model = ModelFactory.createDefaultModel()
        model.read(FileInputStream("src/test/resources/models/literals.nt"), null, "NT")
        assertEquals(10, model.size())
        RDFUtils.removeNonURIObjects(model)
        checkModel(model)
    }

    @Test
    fun `remove literals while streaming model`(){
        val model = RDFUtils.streamNoLiterals("src/test/resources/models/literals.nt")
        checkModel(model)
    }

    private fun checkModel(model: Model){
        //check that all 7 literals were removed
        assertEquals(3, model.size())

        val expectedObjectsRemaining = arrayListOf("https://example.com1", "https://example.com2")
        var foundAnon = false

        //check if all 3 objects are the two uris and 1 blank node and not some literal.
        model.listStatements().forEach {
            assertFalse(it.`object`.isLiteral)
            if(it.`object`.isResource){
                expectedObjectsRemaining.remove(it.`object`.asResource().uri)
            }
            if(it.`object`.isAnon){
                foundAnon=true
            }
        }
        assertTrue(foundAnon, "Blank Node was removed.")
        //all expected objects were found
        assertEquals(0, expectedObjectsRemaining.size, "Some objects were removed from the model: $expectedObjectsRemaining")

    }

    @Test
    fun `check script correctly executed and got correct arguments`(){
        File("tmp_Adsfsdaf3224").deleteOnExit()
        RDFUtils.loadTripleStoreFromScript("file:///abc/def.nt", "src/test/resources/scripts/loader.sh")
        //check that output is correctly written to tmp_Adsfsdaf3224 and has the content def
        val actual = FileUtils.readFileToString(File("tmp_Adsfsdaf3224")).trim()
        val expected = "/abc/ : def.nt"
        assertEquals(expected, actual)
    }

    @Test
    fun `check not allowed scripts to throw error`(){
        //script exists, but is not set to execute
        assertThrows<IOException> { RDFUtils.loadTripleStoreFromScript("doesntmatter", "src/test/resources/scripts/notAllowed.sh") }
    }


}