package app.trainer.feature.chat.di

import app.trainer.feature.chat.presentation.dialog.mvi.DialogScreenModel
import app.trainer.feature.chat.presentation.list.mvi.ChatListScreenModel
import app.trainer.feature.chat.presentation.dialog.ui.DialogScreen
import app.trainer.feature.chat.presentation.list.ui.ChatListScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class ChatFeatureModule {

    val module = module {
        viewModel {
            ChatListScreenModel(chatRepository = get())
        }
        viewModel { (dialogId: String) ->
            DialogScreenModel(
                dialogId = dialogId,
                chatRepository = get(),
                profileRepository = get(),
            )
        }
        screen<Screens.CoachChats> { ChatListScreen() }
        screen<Screens.Chat> { DialogScreen(dialogId = it.dialogId) }
    }
}
