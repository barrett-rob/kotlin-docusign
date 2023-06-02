package org.example

import com.docusign.esign.api.EnvelopesApi
import com.docusign.esign.client.ApiClient
import com.docusign.esign.model.EnvelopeId
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
            fromDate = DateTimeFormatter.ISO_DATE.format(LocalDate.now().minusDays(7))
            include = "recipients"
        }

        val listStatusChanges = envelopesApi.listStatusChanges(
            /* accountId = */ accountId,
            /* options = */ listStatusChangesOptions
        )

        println("signature status:")
        println(extractInfo(listStatusChanges).joinToString("\n"))

        val listAuditEvents = listStatusChanges.envelopes.associate { envelope ->
            envelope.envelopeId to envelopesApi.listAuditEvents(
                /* accountId = */ accountId,
                /* envelopeId = */ envelope.envelopeId
            ).auditEvents.map { envelopeAuditEvent ->
                envelopeAuditEvent.eventFields.map { nameValue ->
                    nameValue.name to nameValue.value
                }
            }
        }

        println("audit events:")
        println(extractInfo(listAuditEvents).joinToString("\n"))
    }

    private fun extractInfo(envelopesInformation: EnvelopesInformation?) =
        envelopesInformation?.envelopes?.map { envelope ->
            val signers = envelope.recipients?.signers?.joinToString(",") { signer -> signer.email }
            "\t${envelope.status} - ${envelope.envelopeId} - ${envelope.emailSubject} - $signers"
        } ?: listOf("\tNo envelopes found")

    private fun extractInfo(auditEventsByEnvelopeId: Map<String, List<List<Pair<String, String>>>>): List<String> {
        return auditEventsByEnvelopeId.entries.map { entry ->
            "${entry.key}: " + entry.value.map { list ->
                list.map { pair ->
                    "\n\t${pair.first} = ${pair.second}"
                }
            }
        }
    }

}
