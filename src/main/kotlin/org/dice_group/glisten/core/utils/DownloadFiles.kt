package org.dice_group.glisten.core.utils

import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File

import java.net.URL

object DownloadFiles {

    /**
     * @return the path to file
     */
    fun download(url: String, destFolder: String): String{
        val destFile = destFolder+url.substringAfterLast("/")
        FileUtils.copyURLToFile(URL(url), File(destFile))
        return destFile
    }

    fun unzipFile(file: String, dest: String){
        ZipFile(file).extractAll(dest)
    }

}