package app.trainer.feature.account.di

import app.trainer.feature.account.contact.mvi.ContactLinkScreenModel
import app.trainer.feature.account.contact.ui.ContactLinkScreen
import app.trainer.feature.account.devices.mvi.DevicesScreenModel
import app.trainer.feature.account.devices.ui.DevicesScreen
import app.trainer.feature.account.invite.mvi.InviteScreenModel
import app.trainer.feature.account.invite.ui.InviteScreen
import app.trainer.feature.account.invitelink.mvi.InviteLinkScreenModel
import app.trainer.feature.account.invitelink.ui.InviteLinkScreen
import app.trainer.feature.account.nocoach.mvi.NoCoachScreenModel
import app.trainer.feature.account.nocoach.ui.NoCoachScreen
import app.trainer.feature.account.onboarding.mvi.OnboardingScreenModel
import app.trainer.feature.account.onboarding.ui.OnboardingScreen
import app.trainer.feature.account.profile.mvi.ProfileScreenModel
import app.trainer.feature.account.profile.ui.ProfileScreen
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
        viewModel { ContactLinkScreenModel(profileRepository = get()) }
        viewModel { DevicesScreenModel(sessionsRepository = get()) }
        viewModel { NoCoachScreenModel(authRepository = get()) }
        viewModel { parameters ->
            WelcomeScreenModel(
                afterSessionExpiry = parameters.get(),
                authRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
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

        screen<Screens.Invite> { InviteScreen(afterSessionExpiry = it.afterSessionExpiry) }
        screen<Screens.InviteLink> { InviteLinkScreen(code = it.code) }
        screen<Screens.Onboarding> { OnboardingScreen(code = it.code) }
        screen<Screens.ContactLink> { ContactLinkScreen() }
        screen<Screens.Profile> { ProfileScreen() }
        screen<Screens.Devices> { DevicesScreen() }
        screen<Screens.NoCoach> { NoCoachScreen() }
        screen<Screens.Welcome> { WelcomeScreen(afterSessionExpiry = it.afterSessionExpiry) }
    }

    companion object {

        const val DEVICE_INFO_QUALIFIER = "deviceInfo"
    }
}
