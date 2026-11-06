package com.eightharsh.eightharsh.core.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.eightharsh.eightharsh.core.theme.LocalSearchPillColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Search Bar for apps list that collapses into a pill
 */
@Composable
fun AnimatedPillSearchBar(
    searchText: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onSearchDone: (String, SoftwareKeyboardController?) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    showLabel: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val searchColors = LocalSearchPillColors.current
    val pillColor = searchColors?.first ?: if (showLabel) colors.primary else colors.secondary
    val pillTextColor = searchColors?.second ?: if (showLabel) colors.onPrimary else colors.onSecondary
    val scope = rememberCoroutineScope()

    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchText)) }

    // Sync internal state with external searchText
    LaunchedEffect(searchText) {
        if (searchText != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = searchText)
        }
    }

    // Animation Specs
    val width by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else if (showLabel) 150.dp else 56.dp,
        label = "widthAnimation",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        )
    )
    val interactionSource =  remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isMorphed by remember { mutableStateOf(false) }
    val radius = if (showLabel) 28.dp else if (isMorphed || isPressed) 16.dp else 28.dp
    val animatedRadius by animateDpAsState(
        targetValue = radius,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow
        ),
        label = "SearchBoxCornerAnimation"
    )

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    androidx.activity.compose.BackHandler(enabled = isExpanded) {
        focusManager.clearFocus()
        keyboardController?.hide()
        onExpandedChange(false)
    }

    // Handle Auto-focus and Expansion changes
    LaunchedEffect(isExpanded, autoFocus) {
        if (isExpanded) {
            delay(150.milliseconds)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Surface(
        shape = RoundedCornerShape(animatedRadius),
        modifier = modifier
            .width(width)
            .height(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (!isMorphed) {
                        isMorphed = true
                        scope.launch {
                            delay(200.milliseconds)
                            isMorphed = false
                        }
                    } else {
                        isMorphed = false
                    }

                    onExpandedChange(!isExpanded)
                }
            ),
        color = pillColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = pillTextColor,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp)
            )

            if (showLabel && !isExpanded) {
                androidx.compose.material3.Text(
                    text = "Search",
                    modifier = Modifier.padding(start = 48.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = pillTextColor
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onSearchTextChanged(it.text)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp, end = if (searchText.isNotEmpty()) 48.dp else 16.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearchDone(textFieldValue.text.trim(), keyboardController)
                    }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = pillTextColor
                    ),
                    cursorBrush = SolidColor(pillTextColor),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            innerTextField()
                        }
                    })
            }
            if (isExpanded && searchText.isNotEmpty()) {
                androidx.compose.material3.IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = {
                        textFieldValue = TextFieldValue("")
                        onSearchTextChanged("")
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search",
                        tint = pillTextColor)
                }
            }

        }
    }
}