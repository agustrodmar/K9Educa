package app.k9mail.core.ui.compose.designsystem.atom.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField as Material3OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Suppress("LongParameterList")
@Composable
fun TextFieldOutlinedEmailAddress(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    isRequired: Boolean = false,
    hasError: Boolean = false,  // parámetro para controlar si el campo se muestra en rojo
) {
    Column {
        Material3OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = isEnabled,
            label = selectLabel(label, isRequired),
            readOnly = isReadOnly,
            isError = hasError,  //  hasError para controlar el estado de error visual
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Email,
            ),
            singleLine = true,
        )
        // Mostrar mensaje de error si existe
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
