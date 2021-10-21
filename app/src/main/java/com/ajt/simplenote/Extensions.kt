package com.ajt.simplenote

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.postDelayed

fun Any.log(msg: String) = Log.d(this::class.java.simpleName, msg)

fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.scaledDensity + 0.5f).toInt()

inline fun Menu.add(
    title: CharSequence,
    itemId: Int = 0,
    group: Int = 0,
    crossinline onClick: (MenuItem) -> Unit
): MenuItem = add(group, itemId, 0, title).apply {
    clearHeaders()
    setOnMenuItemClickListener {
        onClick(this)
        false
    }
}

fun Menu.add(title: Int, itemId: Int = 0, group: Int = 0, onClick: ((MenuItem) -> Unit)? = null): MenuItem =
    add(group, itemId, 0, title).apply {
        clearHeaders()
        setOnMenuItemClickListener {
            onClick?.invoke(this)
            false
        }
    }

fun Menu.addSub(title: Int, group: Int = 0, withMenu: ((SubMenu) -> Unit)? = null): SubMenu =
    addSubMenu(group, 0, 0, title).apply {
        MenuCompat.setGroupDividerEnabled(this, true)
        if (withMenu != null) withMenu(this)
    }

fun Menu.clearHeaders() {
    forEach {
        it.subMenu?.apply {
            clearHeader()
            item?.subMenu?.clearHeaders()
        }
    }
}

fun Context.getThemeColor(res: Int) = TypedValue().apply { theme.resolveAttribute(res, this, true) }.data

fun MenuItem.tintIcon(context: Context) = icon?.setTintList(ColorStateList.valueOf(context.getThemeColor(R.attr.titleTextColor)))

fun Menu.tintIcons(context: Context) {
    children.forEach {
        it.tintIcon(context)
        it.subMenu?.tintIcons(context)
    }
}

fun View.forceShowKB(onVisible: (() -> Unit)? = null) {
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View?) {
            onVisible?.invoke()
            postDelayed(25L) { showKB() }
        }

        override fun onViewDetachedFromWindow(v: View?) {
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
    })
    showKB()
}

val View.inputMethodManager get() = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

fun View.showKB() {
    requestFocus()
    postDelayed(16) { inputMethodManager.showSoftInput(this, 0) }
}
/*

//Universal function for hiding keyboard
fun View.hideKB() {
    clearFocus()
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}*/
