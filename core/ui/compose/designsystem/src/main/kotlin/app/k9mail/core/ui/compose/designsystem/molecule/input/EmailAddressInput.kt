@file:Suppress("SpellCheckingInspection")

package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.R
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlinedEmailAddress

@Composable
fun EmailAddressInput(
    onEmailAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    emailAddress: String = "",
    errorMessage: String? = null,
    isEnabled: Boolean = true,
    contentPadding: PaddingValues = inputContentPadding(),
    showError: Boolean = false  // Mostrar error solo si se activó showError
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = errorMessage,
    ) {

        TextFieldOutlinedEmailAddress(
            value = emailAddress,
            onValueChange = onEmailAddressChange,
            label = stringResource(id = R.string.designsystem_molecule_email_address_input_label),
            isEnabled = isEnabled,
            hasError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            error = if (showError) errorMessage ?: "El correo electrónico introducido no pertenece a educa.madrid.org" else ""
        )


    }
}

@Composable
fun EmailAddressDiscoveryInput(
    onEmailAddressChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    emailAddress: String = "",
    errorMessage: String? = null,  // Mensaje de error pasado desde el ViewModel
    isEnabled: Boolean = true,
    contentPadding: PaddingValues = inputContentPadding(),
    showError: Boolean = false  // Mostrar error solo si se activó showError
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = null,  // No mostrar el mensaje de error en el InputLayout para evitar duplicación
    ) {
        TextFieldOutlinedEmailAddress(
            value = emailAddress,
            onValueChange = onEmailAddressChange,
            label = stringResource(id = R.string.designsystem_molecule_email_address_input_label),
            isEnabled = isEnabled,
            hasError = showError,  // Mostrar el campo en rojo si showError es true
            modifier = Modifier.fillMaxWidth(),
            error = if (showError) {
                if (emailAddress.isBlank()) {
                    "Introduzca su dirección de correo electrónico para continuar."
                } else {
                    errorMessage ?: ("El correo electrónico introducido no pertenece a EducaMadrid: educa.madrid.org")
                }
            } else {
                ""
            }
        )
    }
}
