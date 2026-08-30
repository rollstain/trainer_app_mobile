package app.trainer.android.auth

import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.InviteCode
import app.trainer.data.auth.InvitePreview
import app.trainer.data.auth.LinkedIdentity
import app.trainer.data.auth.TelegramLoginStart
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult

fun failure(kind: RequestFailure, retryAfterSeconds: Long? = null, field: String? = null): RequestResult.Error =
    RequestResult.Error(
        kind = kind,
        statusCode = null,
        userMessage = "",
        devMessage = "подготовлено тестом",
        retryAfterSeconds = retryAfterSeconds,
        fieldErrors = field?.let { mapOf(it to "занято") }.orEmpty(),
    )

/**
 * Отвечает моделям заранее заданными исходами. Контракт с бэком проверяется отдельно,
 * на моке HTTP; здесь важны только состояния экрана, поэтому сеть из тестов убрана совсем.
 */
class FakeAuthRepository : AuthRepository, IdentitiesRepository {

    var signUpAnswer: RequestResult<Unit> = RequestResult.Success(Unit)
    var signInAnswer: RequestResult<Unit> = RequestResult.Success(Unit)
    var forgotAnswer: RequestResult<Unit> = RequestResult.Success(Unit)
    var resetByEmailAnswers: ArrayDeque<RequestResult<Unit>> = ArrayDeque()
    var resetByTelegramAnswer: RequestResult<Unit> = RequestResult.Success(Unit)
    var setPasswordAnswer: RequestResult<Unit> = RequestResult.Success(Unit)
    var confirmEmailAnswer: RequestResult<Unit> = RequestResult.Success(Unit)
    var resendConfirmationAnswer: RequestResult<Unit> = RequestResult.Success(Unit)

    var isSignedIn: Boolean = false
        private set

    val sentLogins = mutableListOf<String?>()

    override suspend fun isAuthorized(): Boolean = isSignedIn

    override suspend fun signUpWithPassword(
        displayName: String,
        email: String,
        login: String?,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        sentLogins += login
        return signUpAnswer.alsoSignInOnSuccess()
    }

    override suspend fun signInWithPassword(
        identifier: String,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> = signInAnswer.alsoSignInOnSuccess()

    override suspend fun requestPasswordReset(email: String): RequestResult<Unit> = forgotAnswer

    override suspend fun resetPasswordByEmail(
        token: String,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> = (resetByEmailAnswers.removeFirstOrNull() ?: RequestResult.Success(Unit))
        .alsoSignInOnSuccess()

    override suspend fun resetPasswordByTelegram(
        claimToken: String,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> = resetByTelegramAnswer.alsoSignInOnSuccess()

    override suspend fun confirmEmail(token: String): RequestResult<Unit> = confirmEmailAnswer

    override suspend fun requestEmailConfirmation(): RequestResult<Unit> = resendConfirmationAnswer

    override suspend fun setPassword(
        email: String?,
        login: String?,
        currentPassword: String?,
        newPassword: String,
    ): RequestResult<Unit> = setPasswordAnswer

    override suspend fun previewInvite(code: String): RequestResult<InvitePreview> = notNeeded()

    override suspend fun redeemInvite(
        code: String,
        displayName: String,
        deviceInfo: String,
    ): RequestResult<Unit> = notNeeded()

    override suspend fun startTelegramLogin(): RequestResult<TelegramLoginStart> = notNeeded()

    override suspend fun signInWithProvider(
        provider: AuthProvider,
        token: String,
        deviceInfo: String,
    ): RequestResult<Unit> = notNeeded()

    override suspend fun joinCoach(code: String): RequestResult<Unit> = notNeeded()

    override suspend fun createInvite(): RequestResult<InviteCode> = notNeeded()

    override suspend fun logout() {
        isSignedIn = false
    }

    override suspend fun linkedIdentities(): RequestResult<List<LinkedIdentity>> = notNeeded()

    override suspend fun linkProvider(provider: AuthProvider, token: String): RequestResult<List<LinkedIdentity>> =
        notNeeded()

    override suspend fun unlinkProvider(provider: AuthProvider): RequestResult<List<LinkedIdentity>> = notNeeded()

    private fun RequestResult<Unit>.alsoSignInOnSuccess(): RequestResult<Unit> {
        if (this is RequestResult.Success) isSignedIn = true
        return this
    }

    private fun <T> notNeeded(): RequestResult<T> =
        error("Этот путь тест не разрешал — если он понадобился, задайте ответ явно")
}
