package app.trainer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntryDecorator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

val LocalNavViewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner?> { null }

@Composable
internal fun rememberViewModelStoreNavEntryDecorator(): NavEntryDecorator<Any> {
    return remember {
        NavEntryDecorator<Any>(
            onPop = { contentKey -> ViewModelRegistry.remove(contentKey) },
            decorate = { entry ->
                val owner = remember(entry.contentKey) {
                    ViewModelRegistry.getOrCreate(entry.contentKey)
                }
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides owner,
                    LocalNavViewModelStoreOwner provides owner,
                ) {
                    entry.Content()
                }
            },
        )
    }
}

@Composable
inline fun <reified T : ViewModel> koinScreenModel(
    qualifier: Qualifier? = null,
    key: String? = null,
    noinline parameters: ParametersDefinition? = null,
): T {
    val owner = LocalNavViewModelStoreOwner.current
        ?: error("ViewModelStoreOwner не найден: добавьте rememberViewModelStoreNavEntryDecorator в toEntries()")
    return koinViewModel(
        qualifier = qualifier,
        key = key,
        viewModelStoreOwner = owner,
        parameters = parameters,
    )
}
