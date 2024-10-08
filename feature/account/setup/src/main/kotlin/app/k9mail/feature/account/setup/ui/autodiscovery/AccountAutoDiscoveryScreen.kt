@file:Suppress("SpellCheckingInspection")

package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.AutoDiscoveryUiResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ViewModel

@Composable
internal fun AccountAutoDiscoveryScreen(
    onNext: (AutoDiscoveryUiResult) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    appNameProvider: AppNameProvider,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.NavigateNext -> onNext(effect.result)
        }
    }

    // Inicialmente no hay errores, pero los controlamos aquí para correo y contraseña
    var showError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    AccountAutoDiscoveryContent(
        state = state.value,
        onEvent = { event ->
            if (event is Event.OnNextClicked) {
                val emailValue = state.value.emailAddress.value
                val passwordValue = state.value.password.value

                // Validamos si el correo está en blanco o no pertenece a @educa.madrid.org
                emailError = when {
                    emailValue.isBlank() -> "Introduzca su dirección de correo electrónico para continuar."
                    !emailValue.endsWith("@educa.madrid.org", ignoreCase = true) ->
                        "La dirección de correo electrónico proporcionada no pertenece a EducaMadrid: @educa.madrid.org"
                    else -> null
                }

                // Validamos si la contraseña está vacía
                passwordError = if (passwordValue.isBlank()) {
                    "Introduzca su contraseña para continuar."
                } else {
                    null
                }

                // Si hay errores en el correo o la contraseña, mostramos el error
                showError = emailError != null || passwordError != null

                if (!showError) {
                    // Si no hay errores, disparamos el evento para continuar
                    dispatch(event)
                }
            } else {
                dispatch(event)
            }
        },
        oAuthViewModel = viewModel.oAuthViewModel,
        appName = appNameProvider.appName,
        modifier = modifier,
        showError = showError  // Pasamos el estado de error para que se muestre si es necesario
    )
}
