package com.chamberly.chamberly.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.Compression
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

suspend fun compressImageFile(context: Context,imageFileUri:Uri,filter:(Compression)->Unit ):File{
    val tempImageFile = getFilePathFromUri(uri = imageFileUri, context = context)
    val compressedImageFile = Compressor.compress(context,File(tempImageFile?.path!!) ) {
     filter(this)


    }
    return compressedImageFile
}
@Throws(IOException::class)
fun getFilePathFromUri(uri: Uri?, context: Context?): Uri? {
    val fileName: String = getFileName(uri, context)
    val file = File(context?.externalCacheDir, fileName)
    file.createNewFile()
    FileOutputStream(file).use { outputStream ->
        context?.contentResolver?.openInputStream(uri!!).use { inputStream ->
            copyFile(inputStream, outputStream)
            outputStream.flush()
        }
    }
    return Uri.fromFile(file)
}

@Throws(IOException::class)
private fun copyFile(`in`: InputStream?, out: OutputStream) {
    val buffer = ByteArray(1024)
    var read: Int? = null
    while (`in`?.read(buffer).also({ read = it!! }) != -1) {
        read?.let { out.write(buffer, 0, it) }
    }
}

fun getFileName(uri: Uri?, context: Context?): String {
    var fileName: String? = getFileNameFromCursor(uri, context)
    if (fileName == null) {
        val fileExtension: String? = getFileExtension(uri, context)
        fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""
    } else if (!fileName.contains(".")) {
        val fileExtension: String? = getFileExtension(uri, context)
        fileName = "$fileName.$fileExtension"
    }
    return fileName
}

fun getFileExtension(uri: Uri?, context: Context?): String? {
    val fileType: String? = context?.contentResolver?.getType(uri!!)
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
}

fun getFileNameFromCursor(uri: Uri?, context: Context?): String? {
    val fileCursor: Cursor? = context?.contentResolver
        ?.query(uri!!, arrayOf<String>(OpenableColumns.DISPLAY_NAME), null, null, null)
    var fileName: String? = null
    if (fileCursor != null && fileCursor.moveToFirst()) {
        val cIndex: Int = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cIndex != -1) {
            fileName = fileCursor.getString(cIndex)
        }
    }
    fileCursor?.close()
    return fileName
}