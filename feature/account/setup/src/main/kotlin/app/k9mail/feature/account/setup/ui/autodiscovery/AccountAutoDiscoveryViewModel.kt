package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.lifecycle.viewModelScope
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.autodiscovery.api.ImapServerSettings
import app.k9mail.autodiscovery.api.IncomingServerSettings
import app.k9mail.autodiscovery.demo.DemoServerSettings
import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.entity.AutoDiscoveryAuthenticationType
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.AutoDiscoveryUiResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ConfigStep
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Error
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.State
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Validator
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions", "SpellCheckingInspection")
internal class AccountAutoDiscoveryViewModel(
    initialState: State = State(),
    private val validator: Validator,
    private val getAutoDiscovery: UseCase.GetAutoDiscovery,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    override val oAuthViewModel: AccountOAuthContract.ViewModel,
) : BaseViewModel<State, Event, Effect>(initialState), AccountAutoDiscoveryContract.ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.EmailAddressChanged -> changeEmailAddress(event.emailAddress)
            is Event.PasswordChanged -> changePassword(event.password)
            is Event.ResultApprovalChanged -> changeConfigurationApproval(event.confirmed)
            is Event.OnOAuthResult -> onOAuthResult(event.result)

            Event.OnNextClicked -> onNext()
            Event.OnBackClicked -> onBack()
            Event.OnRetryClicked -> onRetry()
            Event.OnEditConfigurationClicked -> {
                navigateNext(isAutomaticConfig = false)
            }
        }
    }

    private fun changeEmailAddress(emailAddress: String) {
        accountStateRepository.clear()
        updateState {
            State(
                emailAddress = StringInputField(value = emailAddress),
                isNextButtonVisible = true,
            )
        }
    }

    private fun changePassword(password: String) {
        updateState {
            it.copy(
                password = it.password.updateValue(password),
            )
        }
    }

    private fun changeConfigurationApproval(approved: Boolean) {
        updateState {
            it.copy(
                configurationApproved = it.configurationApproved.updateValue(approved),
            )
        }
    }

    private fun onNext() {
        // Combinamos las validaciones de email y contraseña
        val emailAddressValue = state.value.emailAddress.value
        val passwordValue = state.value.password.value

        // Validamos el email: si está en blanco o no es de EducaMadrid
        if (emailAddressValue.isBlank()) {
            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateError(
                        EmailValidationError("Introduzca su dirección de correo electrónico para continuar.")
                    )
                )
            }
            return
        }

        // Validar dominio permitido
        val isDomainValid = emailAddressValue.endsWith("@educa.madrid.org", ignoreCase = true)
        if (!isDomainValid) {
            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateError(
                        EmailValidationError("La dirección de correo electrónico proporcionada no pertenece a EducaMadrid: @educa.madrid.org")
                    )
                )
            }
            return
        }

        // Validamos el correo utilizando el validador (si es un correo válido, sintácticamente)
        val emailValidationResult = validator.validateEmailAddress(emailAddressValue)
        val passwordValidationResult = validator.validatePassword(passwordValue)

        val isEmailValid = emailValidationResult !is ValidationResult.Failure
        val isPasswordValid = passwordValidationResult !is ValidationResult.Failure

        updateState {
            it.copy(
                emailAddress = it.emailAddress.updateFromValidationResult(emailValidationResult),
                password = it.password.updateFromValidationResult(passwordValidationResult),
            )
        }

        // Si ambos son válidos, procedemos con la detección automática de la configuración
        if (isEmailValid && isPasswordValid) {
            loadAutoDiscovery()  // Función existente que carga la configuración de auto-descubrimiento
        }
    }

    private fun onRetry() {
        updateState {
            it.copy(error = null)
        }
        loadAutoDiscovery()
    }

    private fun submitEmail() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val hasError = emailValidationResult is ValidationResult.Failure

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationResult(emailValidationResult),
                )
            }

            if (!hasError) {
                loadAutoDiscovery()
            }
        }
    }

    private fun loadAutoDiscovery() {
        viewModelScope.launch {
            updateState {
                it.copy(
                    isLoading = true,
                )
            }

            val result = getAutoDiscovery.execute(state.value.emailAddress.value)
            when (result) {
                AutoDiscoveryResult.NoUsableSettingsFound -> updateNoSettingsFound()
                is AutoDiscoveryResult.Settings -> updateAutoDiscoverySettings(result)
                is AutoDiscoveryResult.NetworkError -> updateError(Error.NetworkError)
                is AutoDiscoveryResult.UnexpectedException -> updateError(Error.UnknownError)
            }
        }
    }

    private fun updateNoSettingsFound() {
        updateState {
            it.copy(
                isLoading = false,
                autoDiscoverySettings = null,
                configStep = ConfigStep.MANUAL_SETUP,
            )
        }
    }

    private fun updateAutoDiscoverySettings(settings: AutoDiscoveryResult.Settings) {
        if (settings.incomingServerSettings is DemoServerSettings) {
            updateState {
                it.copy(
                    isLoading = false,
                    autoDiscoverySettings = settings,
                    configStep = ConfigStep.PASSWORD,
                    isNextButtonVisible = true,
                )
            }
            return
        }

        val imapServerSettings = settings.incomingServerSettings as ImapServerSettings
        val isOAuth = imapServerSettings.authenticationTypes.first() == AutoDiscoveryAuthenticationType.OAuth2

        if (isOAuth) {
            oAuthViewModel.initState(
                AccountOAuthContract.State(
                    hostname = imapServerSettings.hostname.value,
                    emailAddress = state.value.emailAddress.value,
                ),
            )
        }

        updateState {
            it.copy(
                isLoading = false,
                autoDiscoverySettings = settings,
                configStep = if (isOAuth) ConfigStep.OAUTH else ConfigStep.PASSWORD,
                isNextButtonVisible = !isOAuth,
            )
        }
    }

    private fun updateError(error: Error) {
        updateState {
            it.copy(
                isLoading = false,
                error = error,
            )
        }
    }

    private fun submitPassword() {
        with(state.value) {
            val emailValidationResult = validator.validateEmailAddress(emailAddress.value)
            val passwordValidationResult = validator.validatePassword(password.value)
            val configurationApprovalValidationResult = validator.validateConfigurationApproval(
                isApproved = configurationApproved.value,
                isAutoDiscoveryTrusted = autoDiscoverySettings?.isTrusted,
            )
            val hasError = listOf(
                emailValidationResult,
                passwordValidationResult,
                configurationApprovalValidationResult,
            ).any { it is ValidationResult.Failure }

            updateState {
                it.copy(
                    emailAddress = it.emailAddress.updateFromValidationResult(emailValidationResult),
                    password = it.password.updateFromValidationResult(passwordValidationResult),
                    configurationApproved = it.configurationApproved.updateFromValidationResult(
                        configurationApprovalValidationResult,
                    ),
                )
            }

            if (!hasError) {
                navigateNext(state.value.autoDiscoverySettings != null)
            }
        }
    }

    private fun onBack() {
        when (state.value.configStep) {
            ConfigStep.EMAIL_ADDRESS -> {
                if (state.value.error != null) {
                    updateState {
                        it.copy(error = null)
                    }
                } else {
                    navigateBack()
                }
            }

            ConfigStep.OAUTH,
            ConfigStep.PASSWORD,
            ConfigStep.MANUAL_SETUP,
            -> updateState {
                it.copy(
                    configStep = ConfigStep.EMAIL_ADDRESS,
                    password = StringInputField(),
                    isNextButtonVisible = true,
                )
            }
        }
    }

    private fun onOAuthResult(result: OAuthResult) {
        if (result is OAuthResult.Success) {
            updateState {
                it.copy(authorizationState = result.authorizationState)
            }

            navigateNext(isAutomaticConfig = true)
        } else {
            updateState {
                it.copy(authorizationState = null)
            }
        }
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext(isAutomaticConfig: Boolean) {
        accountStateRepository.setState(state.value.toAccountState())

        emitEffect(
            Effect.NavigateNext(
                result = mapToAutoDiscoveryResult(
                    isAutomaticConfig = isAutomaticConfig,
                    incomingServerSettings = state.value.autoDiscoverySettings?.incomingServerSettings,
                ),
            ),
        )
    }

    private fun mapToAutoDiscoveryResult(
        isAutomaticConfig: Boolean,
        incomingServerSettings: IncomingServerSettings?,
    ): AutoDiscoveryUiResult {
        val incomingProtocolType = if (incomingServerSettings is ImapServerSettings) {
            IncomingProtocolType.IMAP
        } else {
            null
        }

        return AutoDiscoveryUiResult(
            isAutomaticConfig = isAutomaticConfig,
            incomingProtocolType = incomingProtocolType,
        )
    }
}

/**
 * Clase concreta que va a interactuar con el viewModel para error si se
 * intenta conectar con un email que no es de EducaMadrid.
 */
@Suppress("SpellCheckingInspection")
class EmailValidationError(val message: String) : ValidationError {
    override fun toString(): String {
        return message
    }
}
