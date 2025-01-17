@file:Suppress("SpellCheckingInspection")

package app.k9mail.feature.account.setup.ui.autodiscovery

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.ErrorView
import app.k9mail.core.ui.compose.designsystem.molecule.LoadingView
import app.k9mail.core.ui.compose.designsystem.molecule.input.EmailAddressDiscoveryInput
import app.k9mail.core.ui.compose.designsystem.molecule.input.PasswordInput
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.common.ui.loadingerror.rememberContentLoadingErrorViewState
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.oauth.ui.AccountOAuthView
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.view.AutoDiscoveryResultApprovalView
import app.k9mail.feature.account.setup.ui.autodiscovery.view.AutoDiscoveryResultView

@Composable
internal fun AccountAutoDiscoveryContent(
    state: State,
    onEvent: (Event) -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    appName: String,
    modifier: Modifier = Modifier,
    showError: Boolean = false
) {
    val scrollState = rememberScrollState()

    ResponsiveWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .testTag("AccountAutoDiscoveryContent")
            .then(modifier),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .imePadding(),
            ) {
                AppTitleTopHeader(
                    title = appName,
                )
                Spacer(modifier = Modifier.weight(1f))
                // Pasamos el estado showError a AutoDiscoveryContent
                AutoDiscoveryContent(
                    state = state,
                    onEvent = onEvent,
                    oAuthViewModel = oAuthViewModel,
                    showError = showError
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            WizardNavigationBar(
                onNextClick = { onEvent(Event.OnNextClicked) },
                onBackClick = { onEvent(Event.OnBackClicked) },
                state = WizardNavigationBarState(showNext = state.isNextButtonVisible),
            )
        }
    }
}

@Composable
internal fun AutoDiscoveryContent(
    state: State,
    onEvent: (Event) -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    modifier: Modifier = Modifier,
    showError: Boolean = false  // Añadimos showError aquí
) {
    val resources = LocalContext.current.resources

    ContentLoadingErrorView(
        state = rememberContentLoadingErrorViewState(state),
        loading = {
            LoadingView(
                message = stringResource(id = R.string.account_setup_auto_discovery_loading_message),
                modifier = Modifier.fillMaxSize(),
            )
        },
        error = {
            ErrorView(
                title = stringResource(id = R.string.account_setup_auto_discovery_loading_error),
                message = state.error?.toAutoDiscoveryErrorString(resources),
                onRetry = { onEvent(Event.OnRetryClicked) },
                modifier = Modifier.fillMaxSize(),
            )
        },
        content = {
            ContentView(
                state = state,
                onEvent = onEvent,
                oAuthViewModel = oAuthViewModel,
                resources = resources,
                showError = showError  // Pasamos showError aquí
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    )
}

@Composable
internal fun ContentView(
    state: State,
    onEvent: (Event) -> Unit,
    oAuthViewModel: AccountOAuthContract.ViewModel,
    resources: Resources,
    modifier: Modifier = Modifier,
    showError: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MainTheme.spacings.quadruple)
            .then(modifier),
    ) {
        // Validación condicional: Si estamos en el paso de la dirección de correo electrónico
        if (state.configStep == AccountAutoDiscoveryContract.ConfigStep.EMAIL_ADDRESS) {
            EmailAddressDiscoveryInput(
                emailAddress = state.emailAddress.value,
                errorMessage = state.emailAddress.error?.toAutoDiscoveryValidationErrorString(resources),
                onEmailAddressChange = { onEvent(Event.EmailAddressChanged(it)) },
                contentPadding = PaddingValues(),
                showError = showError
            )

            // Agregamos el campo de contraseña inmediatamente después del correo
            PasswordInput(
                password = state.password.value,
                errorMessage = state.password.error?.toAutoDiscoveryValidationErrorString(resources),
                onPasswordChange = { onEvent(Event.PasswordChanged(it)) },
                contentPadding = PaddingValues(),
            )
        }

        // En caso de configuraciones automáticas o con OAuth
        if (state.configStep != AccountAutoDiscoveryContract.ConfigStep.EMAIL_ADDRESS) {
            AutoDiscoveryResultView(
                settings = state.autoDiscoverySettings,
                onEditConfigurationClick = { onEvent(Event.OnEditConfigurationClicked) },
            )

            // Si no se confía en la configuración, mostramos la vista de aprobación
            if (state.autoDiscoverySettings != null && state.autoDiscoverySettings.isTrusted.not()) {
                AutoDiscoveryResultApprovalView(
                    approvalState = state.configurationApproved,
                    onApprovalChange = { onEvent(Event.ResultApprovalChanged(it)) },
                )
            }

            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
        }

        // Mostrar el OAuth si se necesita
        if (state.configStep == AccountAutoDiscoveryContract.ConfigStep.OAUTH) {
            val isAutoDiscoverySettingsTrusted = state.autoDiscoverySettings?.isTrusted ?: false
            val isConfigurationApproved = state.configurationApproved.value ?: false
            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
            AccountOAuthView(
                onOAuthResult = { result -> onEvent(Event.OnOAuthResult(result)) },
                viewModel = oAuthViewModel,
                isEnabled = isAutoDiscoverySettingsTrusted || isConfigurationApproved,
            )
        }
    }
}

