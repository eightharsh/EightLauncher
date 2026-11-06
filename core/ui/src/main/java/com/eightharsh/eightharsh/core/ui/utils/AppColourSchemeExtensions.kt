package com.eightharsh.eightharsh.core.ui.utils

import androidx.annotation.StringRes
import com.eightharsh.eightharsh.core.theme.colours.AppColourScheme
import com.eightharsh.eightharsh.core.ui.R

@StringRes
fun AppColourScheme.nameRes(): Int = when (this) {
    AppColourScheme.MONOCHROME -> R.string.monochrome
    AppColourScheme.WALLPAPER -> R.string.wallpaper
    AppColourScheme.RED -> R.string.red
    AppColourScheme.GREEN -> R.string.green
    AppColourScheme.BLUE -> R.string.blue
    AppColourScheme.TEAL -> R.string.teal
    AppColourScheme.PURPLE -> R.string.purple
    AppColourScheme.PINK -> R.string.pink
    AppColourScheme.YELLOW -> R.string.yellow
    AppColourScheme.ORANGE -> R.string.orange
    AppColourScheme.SYSTEM -> R.string.system
    AppColourScheme.CHARCOAL_LINEN -> R.string.charcoal_linen
    AppColourScheme.PAPER_INK -> R.string.paper_ink
    AppColourScheme.ESCAPE_THEME -> R.string.eight_launcher
}
