package icfpc2018

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * This class generates a zip archive containing all the
 * files within constant ZIP_DIR.
 * For this example you'll need to put a few files in the
 * directory ZIP_DIR, and it will generate a zip archive
 * containing all those files in the location OUTPUT_ZIP.
 * Minimum Java version: 8
 */
class ZipWriter {
    /**
     * This method creates the zip archive and then goes through
     * each file in the chosen directory, adding each one to the
     * archive. Note the use of the try with resource to avoid
     * any finally blocks.
     */
    fun createZip(dirName: String) {
        // the directory to be zipped
        val directory = Paths.get(dirName)

        // the zip file name that we will create
        val zipFileName = Paths.get(OUTPUT_ZIP).toFile()

        // open the zip stream in a try resource block, no finally needed
        try {
            ZipOutputStream(
                    FileOutputStream(zipFileName)).use { zipStream ->

                // traverse every file in the selected directory and add them
                // to the zip file by calling addToZipFile(..)
                val dirStream = Files.newDirectoryStream(directory)
                dirStream.forEach { path -> addToZipFile(path, zipStream) }

                log.info("Zip file created in " + directory.toFile().path)
            }
        } catch (e: IOException) {
            log.error("Error while zipping.", e)
        } catch (e: ZipParsingException) {
            log.error("Error while zipping.", e)
        }

    }

    /**
     * Adds an extra file to the zip archive, copying in the created
     * date and a comment.
     * @param file file to be archived
     * @param zipStream archive to contain the file.
     */
    private fun addToZipFile(file: Path, zipStream: ZipOutputStream) {
        val inputFileName = file.toFile().path
        try {
            FileInputStream(inputFileName).use { inputStream ->

                // create a new ZipEntry, which is basically another file
                // within the archive. We omit the path from the filename
                val entry = ZipEntry(file.toFile().name)
                entry.creationTime = FileTime.fromMillis(file.toFile().lastModified())
                entry.comment = "Created by TheCodersCorner"
                zipStream.putNextEntry(entry)

                log.info("Generated new entry for: $inputFileName")

                // Now we copy the existing file into the zip archive. To do
                // this we write into the zip stream, the call to putNextEntry
                // above prepared the stream, we now write the bytes for this
                // entry. For another source such as an in memory array, you'd
                // just change where you read the information from.
                val readBuffer = ByteArray(2048)
                var amountRead: Int = inputStream.read(readBuffer)
                var written = 0

                while (amountRead > 0) {
                    zipStream.write(readBuffer, 0, amountRead)
                    written += amountRead

                    amountRead = inputStream.read(readBuffer)
                }

                log.info("Stored $written bytes to $inputFileName")


            }
        } catch (e: IOException) {
            throw ZipParsingException("Unable to process $inputFileName", e)
        }

    }

    /**
     * We want to let a checked exception escape from a lambda that does not
     * allow exceptions. The only way I can see of doing this is to wrap the
     * exception in a RuntimeException. This is a somewhat unfortunate side
     * effect of lambda's being based off of interfaces.
     */
    private inner class ZipParsingException(reason: String, inner: Exception) : RuntimeException(reason, inner)

    companion object {
        val OUTPUT_ZIP = "submit.zip"
    }
}