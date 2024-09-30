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

    var showError by remember { mutableStateOf(false) }  // Inicialmente false
    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    AccountAutoDiscoveryContent(
        state = state.value,
        onEvent = { event ->
            if (event is Event.OnNextClicked) {
                // Validación del email antes de avanzar
                val emailIsValid = state.value.emailAddress.value.endsWith("@educa.madrid.org", ignoreCase = true)
                if (!emailIsValid) {
                    showError = true  // Muestra el error solo cuando el correo es incorrecto
                } else {
                    showError = false  // Si el correo es válido, ocultaa el error
                    dispatch(event)  // Si es válido, dispara el evento normalmente
                }
            } else {
                dispatch(event)
            }
        },
        oAuthViewModel = viewModel.oAuthViewModel,
        appName = appNameProvider.appName,
        modifier = modifier,
        showError = showError  // Pasa el estado de error para que se muestre si es necesario
    )
}
