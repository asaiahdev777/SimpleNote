package com.ajt.simplenote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toFile
import androidx.core.text.getSpans
import androidx.core.view.postDelayed
import com.ajt.simplenote.spans.SerializableImageSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoLoadImageEditText(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {

    lateinit var coroutineScope: CoroutineScope

    private var loadingImages = false
    private val inTestMode = false
    private var disableScroll = false

    init {
        showSoftInputOnFocus = !inTestMode
        isCursorVisible = !inTestMode
    }


    private fun getVisibleImageSpans(): List<SerializableImageSpan> {
        val topLine = layout.getLineForVertical(scrollY)
        val bottomLine = layout.getLineForVertical(scrollY + measuredHeight)
        val start = layout.getLineStart(topLine)
        val end = layout.getLineStart(bottomLine + 1)
        return (text as SpannableStringBuilder).getSpans<SerializableImageSpan>(start, end).toList()
    }

    //TODO come back and clear off-screen bitmaps
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val layout = layout
        if (layout != null && !loadingImages) {

            val visibleImageSpans = getVisibleImageSpans()
            (text as SpannableStringBuilder).getSpans<SerializableImageSpan>().forEach { if (!visibleImageSpans.contains(it)) it.clearMemory() }

            val visibleImageSpansToBeLoaded = visibleImageSpans.filter { it.bitmap == null }

            if (visibleImageSpansToBeLoaded.isNotEmpty()) loadImages(visibleImageSpansToBeLoaded, measuredWidth / 2, measuredHeight / 2)
        }
    }

    private fun loadImages(
        spans: List<SerializableImageSpan>,
        width: Int,
        height: Int
    ) {
        this.log("load images")
        loadingImages = true
        coroutineScope.launch(Dispatchers.IO) {
            spans.forEach { span ->
                if (!span.isVisible) {
                    val uri = span.uri
                    val file = uri.toFile()
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true

                    BitmapFactory.decodeFile(file.absolutePath, options)

                    val fullHeight = options.outHeight
                    val fullWidth = options.outWidth
                    val heightScale = height.toFloat() / fullHeight.toFloat()
                    val widthScale = width.toFloat() / fullWidth.toFloat()
                    val bestScale = if (heightScale < widthScale) heightScale else widthScale

                    options.inSampleSize = (1 / bestScale).toInt() //Determines best scale to fit
                    options.inJustDecodeBounds = false //We now want to actually load the bitmap
                    if (options.inSampleSize % 2 != 0) options.inSampleSize = options.inSampleSize + 1 //

                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    val newWidth = (fullWidth * bestScale).toInt()
                    val newHeight = (fullHeight * bestScale).toInt()
                    if (bitmap != null) span.bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    else span.bitmap = context.getDrawable(R.drawable.loading)?.apply {
                        setTint(Color.RED)
                    }?.toBitmap()
                    span.isVisible = true
                }
            }
            launch(Dispatchers.Main) {
                disableScroll = true
                text = text
                postDelayed(16L) {
                    disableScroll = false
                    loadingImages = false
                }
            }
        }
    }

    override fun bringPointIntoView(offset: Int): Boolean {
        this.log("self-scrolling")
        return if (!disableScroll) super.bringPointIntoView(offset) else false
    }

    /*override fun scrollTo(x: Int, y: Int) {
        if (!disableScroll) super.scrollTo(x, y)
    }

    override fun scrollBy(x: Int, y: Int) {
        if (!disableScroll) super.scrollBy(x, y)
    }

    override fun setScrollY(value: Int) {
        if (!disableScroll) super.setScrollY(value)
    }*/

    override fun getText() = super.getText()!!
}