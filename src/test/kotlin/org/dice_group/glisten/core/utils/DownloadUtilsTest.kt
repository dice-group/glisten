package org.dice_group.glisten.core.utils

import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.*
import org.mockserver.client.server.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import java.io.File
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadUtilsTest {

    lateinit var mockServer: MockServerClient
    lateinit var folder: File

    companion object{
        const val DOWNLOAD_FILE = "src/test/resources/downloads/content.txt"
        val FILE_CONTENT = FileUtils.readFileToString(File(DOWNLOAD_FILE)).trim()

    }

    @BeforeAll
    fun init(){
        folder = File(UUID.randomUUID().toString())

        //create mockserver
        Logger.getLogger("io.netty").level = Level.OFF;
        mockServer = ClientAndServer.startClientAndServer(8001)
        mockServer.`when`(HttpRequest.request().withPath("/myfile")).callback {
            HttpResponse.response(FILE_CONTENT)
        }
    }


    @Test
    fun `given an url link a file should be downloaded if exists`(){
        //check url that exists
        val file = File(DownloadUtils.download("http://localhost:8001/myfile", folder.absolutePath))
        assertTrue(file.exists(), "File $file doesn't exists, hence it wasn't downloaded")
        val content = FileUtils.readFileToString(file).trim()

        assertEquals(FILE_CONTENT, content)
    }

    @Test
    fun `given an url link but a folder which doesn't exists an error shall be thrown`(){
        assertThrows<IOException> { File(DownloadUtils.download("https://localhost/myfile", "423rfsdfarfsafdsfdsafdsaf43")) }
    }

    @Test
    fun `given an url link which doesn't exists should throw an error`(){
        assertThrows<IOException> { File(DownloadUtils.download("https://localhost/doesntexists", ".")) }
    }

    @Test
    fun `given a local url the file should be copied if exists`(){
        val localFileUrl = File(DOWNLOAD_FILE).toURI().toString()
        val file = File(DownloadUtils.download(localFileUrl, folder.absolutePath))
        assertTrue(file.exists(), "File $file doesn't exists, hence it wasn't downloaded")
        val content = FileUtils.readFileToString(file)
        assertEquals(content, FILE_CONTENT)
    }

    @AfterAll
    fun close(){
        mockServer.stop()
        folder.deleteRecursively()
    }
}