package org.dice_group.glisten.core.utils

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

import java.net.URL
import kotlin.jvm.Throws

object DownloadUtils {

    /**
     * Downloads a file located at the url (for local use, use: file:// as the schema)
     * and saves it in the destination folder
     *
     * @param url the url of the file to download
     * @param destFolder the folder to save the file into.
     * @return the path to file
     * @throws IOException if the destination folder doesn't exist or insufficient rights to write in the folder or the Url doesn't exists
     * @throws SecurityException if the permissions to store the file into the destination folder aren't sufficient
     */
    @Throws(IOException::class, SecurityException::class)
    fun download(url: String, destFolder: String): String{
        val destFile = destFolder+File.separator+url.substringAfterLast("/")
        FileUtils.copyURLToFile(URL(url), File(destFile))
        return destFile
    }

    /**
     * Unzips a given file to a destination folder
     *
     * @param file the file to unzip
     * @param dest the folder where the file is unzipped to
     * @throws ZipException if the file is not a zip file
     */
    @Throws(ZipException::class)
    fun unzipFile(file: String, dest: String){
        ZipFile(file).extractAll(dest)
    }

}