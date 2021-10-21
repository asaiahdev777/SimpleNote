package com.ajt.simplenote

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Layout
import android.text.SpannableString
import android.text.style.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ajt.simplenote.spans.*
import kotlinx.android.synthetic.main.note_fragment.*
import java.io.Externalizable
import java.lang.ref.WeakReference
import java.util.*

class NoteFragment : Fragment() {

    private val noteViewModel by lazy { ViewModelProvider(this)[NoteViewModel::class.java] }

    private val pickImageCode = 123
    private val exportDocumentCode = 124

    private val characters = listOf("□ ", "• ", "◦ ", "← ", "↑ ", "→ ", "↓ ")

    private val textSizes = listOf(10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40, 44, 48, 54, 60, 66, 72, 80, 88, 96)
    private val tags = listOf(
        Tag("b") { SerializableStyleSpan(Typeface.BOLD) },
        Tag("i") { SerializableStyleSpan(Typeface.ITALIC) },
        Tag("u") { SerializableUnderlineSpan() },
        Tag("strike") { SerializableStrikethroughSpan() },
        Tag("sub") { SerializableSubscriptSpan() },
        Tag("sup") { SerializableSuperscriptSpan() }
    )

    private val textColors = mutableListOf(
        "#FF0000",
        "#FFA500",
        "#FFFF00",
        "#008000",
        "#0000FF",
        "#4B0082",
        "#EE82EE"
    )

    private var textWatchSuspended = false

    private val logTag get() = this::class.java.simpleName

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.note_fragment, container, false)!!

    override fun onDestroyView() {
        super.onDestroyView()
        with(noteViewModel) {
            selectionStart = editText.selectionStart
            selectionEnd = editText.selectionEnd
            saveNote()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editText.coroutineScope = noteViewModel.viewModelScope
        setHasOptionsMenu(true)
        with(noteViewModel) {
            val observer = object : Observer<Editable> {
                override fun onChanged(value: Editable?) {
                    observableNote.removeObserver(this)
                    loadText(value)
                    configureEditText()
                }
            }
            observableNote.observe(this@NoteFragment, observer)
            loadNote()
        }
    }

    private fun loadText(editable: Editable?) {
        if (editable != null) editText.text = editable
        with(noteViewModel) { editText.setSelection(selectionStart, selectionEnd) }
    }

    private fun configureEditText() {
        editText.background = LinedBackground(editText)
        editText.customSelectionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                tags.forEach { tag ->
                    val name = SpannableString(tag.htmlTag).apply { setSpan(tag.spanFunction(), 0, length, 0) }
                    menu?.add(name) { insertSpan(spanToApply = tag.spanFunction) }
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false

            override fun onDestroyActionMode(mode: ActionMode?) = Unit
        }

        editText.customInsertionActionModeCallback = object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                characters.forEach { character -> menu?.add(character) { insertCharacter(character) } }
                menu?.add(R.string.insertImage) { showImagePicker() }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false

            override fun onDestroyActionMode(mode: ActionMode?) = Unit
        }

        editText.tag = editText.doAfterTextChanged { if (!textWatchSuspended) saveEditText() }
    }

    private fun insertSpan(toggle: Boolean = true, spanToApply: () -> Externalizable) {
        val selectionStart = editText.selectionStart
        val selectionEnd = editText.selectionEnd

        val spanToSearchFor = spanToApply()

        val spans = editText.text.getSpans(selectionStart, selectionEnd, spanToSearchFor::class.java).filter {
            if (it is StyleSpan && spanToSearchFor is StyleSpan) it.style == spanToSearchFor.style
            else true
        }

        fun deleteSpans() = spans.forEach { editText.text.removeSpan(it) }

        fun applySpan() {
            val maxLength = editText.text.lastIndex
            repeat(selectionEnd - selectionStart) {
                val start = selectionStart + it
                val end = (selectionStart + it + 1).coerceAtMost(maxLength)
                Log.d(logTag, "$start..$end")
                editText.text.setSpan(spanToApply(), start, end, 0)
            }
        }

        if (toggle) {
            if (spans.isNotEmpty()) deleteSpans() else applySpan()
        } else {
            deleteSpans()
            applySpan()
        }
        editText.invalidate()
        saveEditText()
    }

    private fun deleteSpan(spanToApply: Class<*>) {
        with(editText.text) { getSpans(editText.selectionStart, editText.selectionEnd, spanToApply)?.forEach { removeSpan(it) } }
        saveEditText()
    }

    private fun saveEditText() = noteViewModel.updateNote(editText.text)

    private fun insertCharacter(character: String) = with(editText) { text.insert(selectionStart, character) }

    private fun showImagePicker() {
        startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }, getString(R.string.pickYourImage)), pickImageCode)
    }

    /* private fun showFolderPicker() {
         val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
             putExtra(Intent.EXTRA_TITLE, getString(R.string.nameYourDocument))
         }
         startActivityForResult(intent, exportDocumentCode)
     }*/

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        showColorMenu(menu, false)
        showColorMenu(menu, true)
        showAlignmentMenu(menu)
        showTextSizeMenu(menu)
        menu.add(R.string.insertImage) { showImagePicker() }
        //menu.add(R.string.exportAsHTMLFile) { showFolderPicker() }
        menu.addSub(R.string.sheets) { subMenu ->
            noteViewModel.sheetsList.forEach { sheet ->
                subMenu.add(sheet.name) { switchSheet(sheet.name) }
            }
            subMenu.add(R.string.newSheet) { showNewSheetDialog() }
        }
        menu.tintIcons(requireContext())
    }

    @SuppressLint("InflateParams")
    private fun showNewSheetDialog() {
        //Create an EditText
        val view = EditText(requireContext()).apply {
            setHint(R.string.sheetName)
            setSingleLine()
            inputType = EditorInfo.TYPE_CLASS_TEXT
            //Force show the KB (my function)
            forceShowKB()
        }

        //Create an AlertDialog builder and run methods on that objects
        AlertDialog.Builder(requireContext()).apply {
            setCancelable(true)
            setView(view)
            setPositiveButton(R.string.save) { _, _ -> with("${view.text}") { if (isNotBlank()) switchSheet(this) } }
        }.show()
    }

    private fun switchSheet(sheet: String) = noteViewModel.updateSheet(sheet) { (activity as MainActivity).switchNote() }

    private fun showColorMenu(menu: Menu, highlight: Boolean) {
        val colorMenu = menu.addSubMenu(if (highlight) R.string.backgroundColor else R.string.textColor)

        colorMenu.item.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setIcon(if (highlight) R.drawable.highlight else R.drawable.palette)
        }

        colorMenu.add(R.string.auto) { deleteSpan(if (highlight) BackgroundColorSpan::class.java else ForegroundColorSpan::class.java) }
        textColors.forEach { color ->
            val colorInt = Color.parseColor(color)
            val spanFunction: () -> Externalizable = { if (highlight) SerializableBackgroundColorSpan(colorInt) else SerializableForegroundColorSpan(colorInt) }
            colorMenu.add(SpannableString(color).apply { setSpan(spanFunction(), 0, length, 0) }) {
                insertSpan(false, spanFunction)
            }
        }
    }

    private fun showAlignmentMenu(menu: Menu) {
        menu.addSub(R.string.alignment) {
            it.add(R.string.left) { insertSpan(false) { SerializableAlignSpan(Layout.Alignment.ALIGN_NORMAL) } }
            it.add(R.string.right) { insertSpan(false) { SerializableAlignSpan(Layout.Alignment.ALIGN_OPPOSITE) } }
            it.add(R.string.center) { insertSpan(false) { SerializableAlignSpan(Layout.Alignment.ALIGN_CENTER) } }
        }.item.apply {
            setIcon(R.drawable.align)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    private fun showTextSizeMenu(menu: Menu) {
        menu.addSub(R.string.textSize) { subMenu ->
            textSizes.forEach { textSize ->
                subMenu.add("$textSize") { insertSpan(false) { SerializableAbsoluteSizeSpan(textSize) } }
            }
        }.item.apply {
            setIcon(R.drawable.text_size)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode == Activity.RESULT_OK && uri != null) {
            if (requestCode == pickImageCode) noteViewModel.appendImage(WeakReference(editText), uri)
            else if (requestCode == exportDocumentCode) noteViewModel.performExport(uri)
        }
    }

}