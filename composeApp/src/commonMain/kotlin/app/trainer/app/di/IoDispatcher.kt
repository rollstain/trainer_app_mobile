package app.trainer.app.di

import kotlinx.coroutines.CoroutineDispatcher

expect fun ioDispatcher(): CoroutineDispatcher
