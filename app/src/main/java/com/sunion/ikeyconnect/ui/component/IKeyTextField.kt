package com.sunion.ikeyconnect.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunion.ikeyconnect.R

@Composable
fun IKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    hasError: Boolean = false,
    protected: Boolean = false,
    textStyle: TextStyle = TextStyle(
        color = colorResource(id = R.color.black),
//        color = colorResource(id = R.color.onSurface),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (hasError) colorResource(id = R.color.redE60a17)
                            else MaterialTheme.colors.primary
                        ),
                        RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                innerTextField()
                if (value.isEmpty())
                    Text(
                        text = hint,
                        style = textStyle.copy(color = colorResource(id = R.color.grayACBECA))
                    )
            }
        },
        visualTransformation = if (protected) PasswordVisualTransformation()
        else VisualTransformation.None,
        modifier = modifier,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Preview(showBackground = true, name = "Empty value")
@Composable
private fun Preview() {
//    FuhsingSmartLockV2AndroidTheme {
        IKeyTextField(
            value = "",
            onValueChange = {},
            hasError = false,
            hint = "example@mail.com",
            modifier = Modifier.padding(5.dp)
        )
//    }
}

@Preview(showBackground = true, name = "Has value")
@Composable
private fun Preview2() {
//    FuhsingSmartLockV2AndroidTheme {
        IKeyTextField(
            value = "example@mail.com",
            onValueChange = {},
            hasError = false,
            hint = "example@mail.com",
            modifier = Modifier.padding(5.dp)
        )
//    }
}

@Preview(showBackground = true, name = "Has value, protected")
@Composable
private fun Preview3() {
//    FuhsingSmartLockV2AndroidTheme {
        IKeyTextField(
            value = "example@mail.com",
            onValueChange = {},
            hasError = false,
            hint = "example@mail.com",
            modifier = Modifier.padding(5.dp),
            protected = true
        )
//    }
}

@Preview(showBackground = true, name = "Has value, error")
@Composable
private fun Preview4() {
//    FuhsingSmartLockV2AndroidTheme {
        IKeyTextField(
            value = "example@mail.com",
            onValueChange = {},
            hasError = true,
            hint = "example@mail.com",
            modifier = Modifier.padding(5.dp)
        )
//    }
}