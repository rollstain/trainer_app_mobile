package app.trainer.data.profile.impl

import app.trainer.data.profile.CoachAccount
import app.trainer.data.profile.CoachAccountCard
import app.trainer.data.profile.OwnerRepository
import app.trainer.entities.Paged
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.safePagedRequest
import app.trainer.network.safeRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val LOG_TAG = "owner"

@Serializable
data class OwnerCoachResponse(
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("displayName")
    val displayName: String?,
    @SerialName("createdAt")
    val createdAt: String?,
    @SerialName("activeClients")
    val activeClients: Int?,
    @SerialName("isOwner")
    val isOwner: Boolean?,
)

@Serializable
data class OwnerCoachCardResponse(
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("displayName")
    val displayName: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("phone")
    val phone: String?,
    @SerialName("login")
    val login: String?,
    @SerialName("zoneId")
    val zoneId: String?,
    @SerialName("createdAt")
    val createdAt: String?,
    @SerialName("activeClients")
    val activeClients: Int?,
    @SerialName("archivedClients")
    val archivedClients: Int?,
    @SerialName("lastSeenAt")
    val lastSeenAt: String?,
    @SerialName("hasPassword")
    val hasPassword: Boolean?,
    @SerialName("providers")
    val providers: List<String>?,
    @SerialName("isOwner")
    val isOwner: Boolean?,
)

class OwnerRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val logger: Logger,
) : OwnerRepository {

    override suspend fun coaches(limit: Int?, after: String?): RequestResult<Paged<List<CoachAccount>>> {
        val loaded = safePagedRequest<List<OwnerCoachResponse>> {
            httpClientProvider.client.get("owner/coaches") {
                limit?.let { parameter("limit", it) }
                after?.let { parameter("after", it) }
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                Paged(
                    items = loaded.data.items.mapNotNull(::toCoachAccount),
                    nextCursor = loaded.data.nextCursor,
                )
            )
        }
    }

    override suspend fun coach(coachId: String): RequestResult<CoachAccountCard> {
        val loaded = safeRequest<OwnerCoachCardResponse> {
            httpClientProvider.client.get("owner/coaches/$coachId")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> toCoachCard(loaded.data)
        }
    }

    private fun toCoachAccount(response: OwnerCoachResponse): CoachAccount? {
        val account = accountOrNull(response)
        if (account == null) {
            logger.error(tag = LOG_TAG, message = "Тренер без обязательных полей пропущен: ${response.coachId}")
        }
        return account
    }

    private fun accountOrNull(response: OwnerCoachResponse): CoachAccount? = CoachAccount(
        coachId = response.coachId ?: return null,
        displayName = response.displayName ?: return null,
        createdAtIso = response.createdAt ?: return null,
        activeClients = response.activeClients ?: return null,
        isOwner = response.isOwner ?: return null,
    )

    private fun toCoachCard(response: OwnerCoachCardResponse): RequestResult<CoachAccountCard> {
        val card = cardOrNull(response)
        if (card == null) {
            logger.error(tag = LOG_TAG, message = "В карточке тренера не хватает полей")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "Ответ owner/coaches/{id} не удалось разобрать в CoachAccountCard",
            )
        }
        return RequestResult.Success(card)
    }

    private fun cardOrNull(response: OwnerCoachCardResponse): CoachAccountCard? {
        return CoachAccountCard(
            coachId = response.coachId ?: return null,
            displayName = response.displayName ?: return null,
            email = response.email,
            phone = response.phone,
            login = response.login,
            zoneId = response.zoneId ?: return null,
            createdAtIso = response.createdAt ?: return null,
            activeClients = response.activeClients ?: return null,
            archivedClients = response.archivedClients ?: return null,
            lastSeenAtIso = response.lastSeenAt,
            hasPassword = response.hasPassword ?: return null,
            providers = response.providers ?: return null,
            isOwner = response.isOwner ?: return null,
        )
    }
}
