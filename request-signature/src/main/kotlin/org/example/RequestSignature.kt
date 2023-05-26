package org.example

import com.docusign.esign.api.EnvelopesApi
import com.docusign.esign.client.ApiClient
import com.docusign.esign.model.Document
import com.docusign.esign.model.EnvelopeDefinition
import com.docusign.esign.model.Recipients
import com.docusign.esign.model.SignHere
import com.docusign.esign.model.Signer
import com.docusign.esign.model.Tabs
import java.util.Base64
import java.util.Properties

fun main(args: Array<String>) {
    RequestSignature().run(args.toList())
}

class RequestSignature {

    fun run(args: List<String>) {
        val signerEmail = args[0]
        val signerName = args.getOrElse(1) { signerEmail.split("@")[0] }
        println("Requesting signature from: $signerName <$signerEmail>")

        val documentContent = RequestSignature::class.java.getResource("/document.txt")?.readText()
            ?: throw RuntimeException("unable to load document.txt")

        val versionLine = documentContent.lines()[0]
        val documentId = versionLine.last().toString()

        val properties = Properties().apply {
            val appConfig = RequestSignature::class.java.getResourceAsStream("/app.config")
                ?: throw RuntimeException("unable to load app.config")
            load(appConfig)
        }

        val rsaKeyFile = properties.getProperty("rsaKeyFile")
        val privateKeyBytes = RequestSignature::class.java.getResourceAsStream("/$rsaKeyFile")?.readBytes()
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

        val signHere = SignHere().apply {
            setDocumentId(documentId)
            pageNumber = "1"
            xPosition = "200"
            yPosition = "100"
        }

        val tabs = Tabs().apply {
            signHereTabs = listOf(signHere)
        }

        val signer = Signer().apply {
            email = signerEmail
            name = signerName
            recipientId = "1"
            setTabs(tabs)
        }

        val recipients = Recipients().apply {
            signers = listOf(signer)
        }

        val document = Document().apply {
            setDocumentId(documentId)
            documentBase64 = Base64.getEncoder().encodeToString(documentContent.toByteArray())
            name = "document.txt"
            fileExtension = "txt"
        }

        val envelopeDefinition = EnvelopeDefinition().apply {
            emailSubject = "Please sign to approve: $versionLine"
            status = "sent"
            setRecipients(recipients)
            documents = listOf(document)
        }

        apiClient.addDefaultHeader("Authorization", "Bearer $accessToken")

        val envelopesApi = EnvelopesApi(apiClient)
        val envelopeSummary = envelopesApi.createEnvelope(
            /* accountId = */ accountId,
            /* envelopeDefinition = */ envelopeDefinition
        )

        println("envelope summary: $envelopeSummary")

    }

}
