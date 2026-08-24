package app.trainer.feature.account.di

import app.trainer.feature.account.contact.mvi.ContactLinkScreenModel
import app.trainer.feature.account.contact.ui.ContactLinkScreen
import app.trainer.feature.account.invite.mvi.InviteScreenModel
import app.trainer.feature.account.invite.ui.InviteScreen
import app.trainer.feature.account.profile.mvi.ProfileScreenModel
import app.trainer.feature.account.profile.ui.ProfileScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class AccountFeatureModule {

    val module = module {
        viewModel { (prefilledCode: String) ->
            InviteScreenModel(
                prefilledCode = prefilledCode.ifEmpty { null },
                authRepository = get(),
                deviceInfo = get(named(DEVICE_INFO_QUALIFIER)),
            )
        }
        viewModel { ContactLinkScreenModel(profileRepository = get()) }
        viewModel {
            ProfileScreenModel(
                profileRepository = get(),
                participantsRepository = get(),
                authRepository = get(),
            )
        }

        screen<Screens.Invite> { InviteScreen(code = it.code) }
        screen<Screens.ContactLink> { ContactLinkScreen() }
        screen<Screens.Profile> { ProfileScreen() }
    }

    companion object {

        const val DEVICE_INFO_QUALIFIER = "deviceInfo"
    }
}
