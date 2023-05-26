# kotlin-docusign
Kotlin example of a docusign cli. Based on a code sample downloaded from [https://developers.docusign.com/](https://developers.docusign.com/).

To run:

1. sign up for a docusign developer account
2. get a clientId, userId and private key
3run using `./gradlew :request-signature:run` or your favourite IDE

Setup:

1. In the subproject you want to run, add an `app.config` file at `src/main/resources`. e.g. 

```text
clientId=...
userId=...
rsaKeyFile=private.key
```

2. And also add your private key file at `src/main/resources/private.key`
