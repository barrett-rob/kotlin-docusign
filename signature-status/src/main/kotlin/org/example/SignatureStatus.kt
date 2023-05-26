package org.example

import com.docusign.esign.api.EnvelopesApi
import com.docusign.esign.client.ApiClient
import com.docusign.esign.model.EnvelopesInformation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

fun main() {
    SignatureStatus().run()
}

class SignatureStatus {

    fun run() {
        println("Requesting signature statuses")

        val properties = Properties().apply {
            val appConfig = SignatureStatus::class.java.getResourceAsStream("/app.config")
                ?: throw RuntimeException("unable to load app.config")
            load(appConfig)
        }

        val rsaKeyFile = properties.getProperty("rsaKeyFile")
        val privateKeyBytes = SignatureStatus::class.java.getResourceAsStream("/$rsaKeyFile")?.readBytes()
            ?: throw RuntimeException("unable to load $rsaKeyFile")

        val apiClient = ApiClient("https://demo.docusign.net/restapi").apply {
            setOAuthBasePath("account-d.docusign.com")
        }

        val oAuthToken = apiClient.requestJWTUserToken(
            /* clientId = */ properties.getProperty("clientId"),
            /* userId = */ properties.getProperty("userId"),
            /* scopes = */ listOf("signature", "impersonation"),
            /* rsaPrivateKey = */ privateKeyBytes,
            /* expiresIn = */ 3600
        )

        val accessToken: String = oAuthToken.accessToken
        val userInfo = apiClient.getUserInfo(accessToken)
        val accountId = userInfo.accounts[0].accountId

        apiClient.addDefaultHeader("Authorization", "Bearer $accessToken")

        val envelopesApi = EnvelopesApi(apiClient)
        val listStatusChangesOptions = envelopesApi.ListStatusChangesOptions().apply {
            fromDate = DateTimeFormatter.ISO_DATE.format(LocalDate.now().minusMonths(6))
            include = "recipients"
        }

        val listStatusChanges = envelopesApi.listStatusChanges(
            /* accountId = */ accountId,
            /* options = */ listStatusChangesOptions
        )

        println("signature status:")
        println(extractInfo(listStatusChanges).joinToString("\n"))
    }

    private fun extractInfo(listStatusChanges: EnvelopesInformation) =
        listStatusChanges.envelopes.map { envelope ->
            val signers = envelope.recipients?.signers?.joinToString(",") { signer -> signer.email }
            "\t${envelope.status} - ${envelope.emailSubject} - $signers"
        }

}
