package app.trainer.feature.account.di

import app.trainer.feature.account.coachsetup.mvi.CoachSetupScreenModel
import app.trainer.feature.account.coachsetup.ui.CoachSetupScreen
import app.trainer.feature.account.contact.mvi.ContactLinkScreenModel
import app.trainer.feature.account.contact.ui.ContactLinkScreen
import app.trainer.feature.account.devices.mvi.DevicesScreenModel
import app.trainer.feature.account.devices.ui.DevicesScreen
import app.trainer.feature.account.identities.mvi.LoginMethodsScreenModel
import app.trainer.feature.account.identities.ui.LoginMethodsScreen
import app.trainer.feature.account.invite.mvi.InviteScreenModel
import app.trainer.feature.account.invite.ui.InviteScreen
import app.trainer.feature.account.invitelink.mvi.InviteLinkScreenModel
import app.trainer.feature.account.invitelink.ui.InviteLinkScreen
import app.trainer.feature.account.newpassword.mvi.NewPasswordScreenModel
import app.trainer.feature.account.newpassword.ui.NewPasswordScreen
import app.trainer.feature.account.nocoach.mvi.NoCoachScreenModel
import app.trainer.feature.account.nocoach.ui.NoCoachScreen
import app.trainer.feature.account.onboarding.mvi.OnboardingScreenModel
import app.trainer.feature.account.onboarding.ui.OnboardingScreen
import app.trainer.feature.account.passwordform.mvi.PasswordFormScreenModel
import app.trainer.feature.account.passwordform.ui.PasswordFormScreen
import app.trainer.feature.account.profile.mvi.ProfileScreenModel
import app.trainer.feature.account.profile.ui.ProfileScreen
import app.trainer.feature.account.recovery.mvi.RecoveryScreenModel
import app.trainer.feature.account.recovery.ui.RecoveryScreen
import app.trainer.feature.account.signin.mvi.SignInScreenModel
import app.trainer.feature.account.signin.ui.SignInScreen
import app.trainer.feature.account.signup.mvi.SignUpScreenModel
import app.trainer.feature.account.signup.ui.SignUpScreen
import app.trainer.feature.account.telegram.TelegramConfirmation
import app.trainer.feature.account.telegramlink.mvi.TelegramLinkScreenModel
import app.trainer.feature.account.telegramlink.ui.TelegramLinkScreen
import app.trainer.feature.account.welcome.mvi.WelcomeScreenModel
import app.trainer.feature.account.welcome.ui.WelcomeScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class AccountFeatureModule {

    val module = module {
        viewModel { parameters ->
            InviteScreenModel(
                afterSessionExpiry = parameters.get(),
                authRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel { parameters ->
            InviteLinkScreenModel(
                code = parameters.get(),
                authRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel { parameters ->
            OnboardingScreenModel(
                code = parameters.get(),
                authRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        factory {
            TelegramConfirmation(
                authRepository = get(),
                identitiesRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel {
            SignInScreenModel(
                authRepository = get(),
                telegramConfirmation = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel {
            SignUpScreenModel(
                authRepository = get(),
                freshSignUp = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel {
            TelegramLinkScreenModel(
                freshSignUp = get(),
                telegramConfirmation = get(),
            )
        }
        viewModel { parameters ->
            RecoveryScreenModel(
                email = parameters.get(),
                authRepository = get(),
                telegramConfirmation = get(),
            )
        }
        viewModel { parameters ->
            NewPasswordScreenModel(
                resetToken = parameters.getOrNull(),
                claimToken = parameters.getOrNull(),
                authRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel { ContactLinkScreenModel(profileRepository = get()) }
        viewModel { DevicesScreenModel(sessionsRepository = get()) }
        viewModel {
            LoginMethodsScreenModel(
                identitiesRepository = get(),
                authRepository = get(),
                profileRepository = get(),
            )
        }
        viewModel { PasswordFormScreenModel(identitiesRepository = get(), profileRepository = get()) }
        viewModel { NoCoachScreenModel(authRepository = get(), profileRepository = get()) }
        viewModel { CoachSetupScreenModel(profileRepository = get()) }
        viewModel { parameters ->
            WelcomeScreenModel(
                afterSessionExpiry = parameters.get(),
                telegramConfirmation = get(),
            )
        }
        viewModel {
            ProfileScreenModel(
                profileRepository = get(),
                participantsRepository = get(),
                authRepository = get(),
                restIntervalStore = get(),
            )
        }

        screen<Screens.SignIn> { SignInScreen() }
        screen<Screens.SignUp> { SignUpScreen() }
        screen<Screens.TelegramLink> { TelegramLinkScreen() }
        screen<Screens.PasswordRecovery> { RecoveryScreen(email = it.email) }
        screen<Screens.NewPassword> { NewPasswordScreen(resetToken = it.resetToken, claimToken = it.claimToken) }
        screen<Screens.Invite> { InviteScreen(afterSessionExpiry = it.afterSessionExpiry) }
        screen<Screens.InviteLink> { InviteLinkScreen(code = it.code) }
        screen<Screens.Onboarding> { OnboardingScreen(code = it.code) }
        screen<Screens.ContactLink> { ContactLinkScreen() }
        screen<Screens.Profile> { ProfileScreen() }
        screen<Screens.Devices> { DevicesScreen() }
        screen<Screens.LoginMethods> { LoginMethodsScreen() }
        screen<Screens.PasswordForm> { PasswordFormScreen() }
        screen<Screens.NoCoach> { NoCoachScreen() }
        screen<Screens.CoachSetup> { CoachSetupScreen() }
        screen<Screens.Welcome> { WelcomeScreen(afterSessionExpiry = it.afterSessionExpiry) }
    }

    companion object {

        const val DEVICE_INFO_QUALIFIER = "deviceInfo"
    }
}
