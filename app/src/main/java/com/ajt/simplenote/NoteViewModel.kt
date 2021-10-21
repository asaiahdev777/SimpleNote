package com.ajt.simplenote

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajt.simplenote.spans.SerializableImageSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Externalizable
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class NoteViewModel : ViewModel() {

    val observableNote = MutableLiveData<Editable>()

    private var loadedText = Editable.Factory.getInstance().newEditable("")

    private val context get() = ApplicationSingleton.app

    private val autoSavePeriod = TimeUnit.SECONDS.toMillis(30)

    private val timerTask by lazy { timerTask { saveNote() } }

    private val autoSaveTimer by lazy { Timer().apply { scheduleAtFixedRate(timerTask, autoSavePeriod, autoSavePeriod) } }

    private val sharedPreferences by lazy { context.getSharedPreferences("Preferences", Context.MODE_PRIVATE) }

    private val sharedPreferencesEditor by lazy { sharedPreferences.edit() }

    private val defaultSheetName by lazy { context.getString(R.string.defaultSheet) }

    private val lastSheet by lazy { sharedPreferences.getString(lastSheetKey, defaultSheetName) ?: defaultSheetName }

    private val rootFolder by lazy { File(context.filesDir, lastSheet) }

    private val savedTextFile
        get() = File(rootFolder, "Text.ntx").apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }

    private val imageFolder get() = File(rootFolder, "images").apply { if (!exists()) mkdirs() }

    companion object {
        const val lastSheetKey = "LastSheet"
    }

    var selectionStart = 0
    var selectionEnd = 0

    init {
        autoSaveTimer
        savedTextFile
    }

    @Synchronized
    fun saveNote() {
        synchronized(savedTextFile) {
            savedTextFile.delete()
            savedTextFile.createNewFile()

            val objectOutputStream = ObjectOutputStream(savedTextFile.outputStream().buffered())
            objectOutputStream.writeObject("$loadedText")

            val spans = loadedText.getSpans<Externalizable>()
            objectOutputStream.writeObject(spans)

            val spanLocations = mutableListOf<Int>()
            spans.forEach {
                spanLocations.add(loadedText.getSpanStart(it))
                spanLocations.add(loadedText.getSpanEnd(it))
            }
            objectOutputStream.writeObject(spanLocations)
            objectOutputStream.close()
        }
        Log.d(this::class.java.simpleName, "Saved!")
    }

    @Suppress("UNCHECKED_CAST")
    fun loadNote() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val objectInputStream = ObjectInputStream(savedTextFile.inputStream().buffered())
                loadedText = SpannableStringBuilder((objectInputStream.readObject() as String))

                val spans = objectInputStream.readObject() as Array<Externalizable>
                val spanLocations = (objectInputStream.readObject() as MutableList<Int>).chunked(2) { it.first()..it.last() }

                spans.forEachIndexed { index, span ->
                    val range = spanLocations[index]
                    loadedText.setSpan(span, range.first, range.last, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //loadedText = sharedPreferences.getString(noteKey, "") ?: ""
            observableNote.postValue(loadedText)
        }
    }

    fun updateNote(note: Editable) {
        loadedText = note
        //viewModelScope.launch(Dispatchers.IO) { saveNote() }
    }

    fun appendImage(reference: WeakReference<EditText>, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val name = resolver.query(uri, null, null, null, null, null)?.use {
                it.moveToFirst()
                it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            } ?: ""

            val newPath = File(imageFolder, "${Date().time}_" + name)

            if (newPath.createNewFile()) {
                val bufferedOutputStream = newPath.outputStream()
                resolver.openInputStream(uri)?.use {
                    it.copyTo(bufferedOutputStream)
                }
                //val newUri = resolver.insert(newPath.toUri(), null)
                launch(Dispatchers.Main) {
                    val editText = reference.get()
                    if (editText != null/* && newUri != null*/) {
                        val start = editText.selectionEnd
                        editText.text = editText.text.apply {
                            insert(start, "\t")
                            setSpan(SerializableImageSpan(newPath.toUri()), start, start + 1, 0)
                        }
                    }
                }
            }
        }
    }

    fun performExport(uri: Uri) {
        /*viewModelScope.launch(Dispatchers.IO) {
            try {
                val folderToMove = File(context.cacheDir, lastSheet)
                folderToMove.mkdirs()

                val htmlFile = File(folderToMove, "$lastSheet.html")
                htmlFile.createNewFile()
                htmlFile.writeText(HtmlCompat.toHtml(loadedText, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE))

                imageFolder.listFiles()?.forEach { it.copyTo(File(folderToMove, it.name), true) }

                val documentUri = DocumentsContract.buildDocumentUri(uri.authority, DocumentsContract.getDocumentId(imageFolder.toUri()))

                DocumentsContract.copyDocument(context.contentResolver, documentUri , uri)

                launch(Dispatchers.Main) {
                    Toast.makeText(context, R.string.documentExportedSuccessfully, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, R.string.failedExportDocument, Toast.LENGTH_SHORT).show()
                }
            }
        }*/
    }

    val sheetsList get() = (rootFolder.parentFile?.listFiles { file: File -> file.isDirectory/* && file.name != lastSheet*/ } ?: emptyArray()).toList()

    fun updateSheet(sheet: String, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferencesEditor.putString(lastSheetKey, sheet)
            sharedPreferencesEditor.commit()
            launch(Dispatchers.Main) { onDone() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        this.log("Cancelled!")
        autoSaveTimer.cancel()
        timerTask.cancel()
    }
}