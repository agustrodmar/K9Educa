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
    errorMessage: String? = null,  // Solo se mostrará cuando sea necesario
    isEnabled: Boolean = true,
    contentPadding: PaddingValues = inputContentPadding(),
    showError: Boolean = false  // Mostrar error solo si se activó showError
) {
    InputLayout(
        modifier = modifier,
        contentPadding = contentPadding,
        errorMessage = if (showError) errorMessage else null,  // Mostrar error solo si showError es true
    ) {
        TextFieldOutlinedEmailAddress(
            value = emailAddress,
            onValueChange = onEmailAddressChange,
            label = stringResource(id = R.string.designsystem_molecule_email_address_input_label),
            isEnabled = isEnabled,
            hasError = showError,  // Ahora usamos hasError para controlar el estado visual
            modifier = Modifier.fillMaxWidth(),
            error = if (showError) errorMessage ?: "El correo electrónico introducido no pertenece a educa.madrid.org" else ""  // Mostrar mensaje de error si showError es true
        )
    }
}
