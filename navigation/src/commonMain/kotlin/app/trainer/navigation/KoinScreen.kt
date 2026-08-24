package app.trainer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.serializer
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.module._singleInstanceFactory
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.navigation3.navigation

fun interface NavKeyProviderInstaller<T : NavKey> {

    fun build(builder: PolymorphicModuleBuilder<T>)
}

@OptIn(KoinInternalApi::class, KoinExperimentalAPI::class, InternalSerializationApi::class)
inline fun <reified T : NavKey> Module.screen(
    metadata: Map<String, Any> = emptyMap(),
    noinline definition: (T) -> Screen,
) {
    val content: @Composable Scope.(T) -> Unit = { definition(it).Content() }
    val installer = _singleInstanceFactory<NavKeyProviderInstaller<T>>(
        qualifier = named<T>(),
        definition = { NavKeyProviderInstaller { it.subclass(T::class, T::class.serializer()) } },
    )
    indexPrimaryType(installer)
    navigation(metadata, content)
}
