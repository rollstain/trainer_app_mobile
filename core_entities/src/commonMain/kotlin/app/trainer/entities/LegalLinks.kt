package app.trainer.entities

private const val PRIVACY_PATH = "legal/privacy"
private const val TERMS_PATH = "legal/terms"

class LegalLinks(baseUrl: String) {

    private val root: String = baseUrl.trimEnd('/') + "/"

    val privacy: String = root + PRIVACY_PATH

    val terms: String = root + TERMS_PATH
}
