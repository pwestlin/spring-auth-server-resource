package com.example.demo

import com.fasterxml.jackson.annotation.JsonAlias
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DemoApplicationTest(
    @Autowired private val authProperties: AuthProperties
) {
    private val restClient = RestClient.builder()
        .baseUrl("http://localhost:8080")
        .build()

    @Test
    fun `hämta token med felaktig inloggning`() {
        val json = restClient
            .post()
            .uri("/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, basicAuth("user", "password"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("""grant_type=client_credentials&scope=read""")
            .retrieve()
            .onStatus({ true }) { _, _ -> }
            .requiredBody<String>()

        assertThat(json).isEqualTo("""{"error":"invalid_client"}""")
    }

    @Test
    fun `hämta token för user med fel scope`() {
        val userClient = authProperties.clients.first { it.clientId == "user" }

        val json = restClient
            .post()
            .uri("/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, basicAuth(userClient.clientId, userClient.clientSecret))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("""grant_type=client_credentials&scope=fisksoppa""")
            .retrieve()
            .onStatus({ true }) { _, _ -> }
            .requiredBody<String>()
        assertThat(json).isEqualTo("""{"error":"invalid_scope"}""")
    }

    @Test
    fun `hämta token för user`() {
        val userClient = authProperties.clients.first { it.clientId == "user" }

        val response = restClient
            .post()
            .uri("/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, basicAuth(userClient.clientId, userClient.clientSecret))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("""grant_type=client_credentials&scope=${scopes(userClient.scopes)}""")
            .retrieve()
            .onStatus({ true }) { _, _ -> }
            .requiredBody<AccessTokenResponse>()

        AccessTokenResponseAssert.assertThat(response)
            .accessTokenIsNotNull()
            .tokenTypeIsBearer()
            .hasScopes(userClient.scopes)
            .expiresInIs299()
    }

    @Test
    fun `hämta token för admin med alla scopes`() {
        val adminClient = authProperties.clients.first { it.clientId == "admin" }

        val response = restClient
            .post()
            .uri("/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, basicAuth(adminClient.clientId, adminClient.clientSecret))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("""grant_type=client_credentials&scope=${scopes(adminClient.scopes)}""")
            .retrieve()
            .onStatus({ true }) { _, _ -> }
            .requiredBody<AccessTokenResponse>()

        AccessTokenResponseAssert.assertThat(response)
            .accessTokenIsNotNull()
            .tokenTypeIsBearer()
            .hasScopes(adminClient.scopes)
            .expiresInIs299()
    }

    @Test
    fun `hämta token för admin med första scopet`() {
        val adminClient = authProperties.clients.first { it.clientId == "admin" }

        val response = restClient
            .post()
            .uri("/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, basicAuth(adminClient.clientId, adminClient.clientSecret))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("""grant_type=client_credentials&scope=${adminClient.scopes.first()}""")
            .retrieve()
            .onStatus({ true }) { _, _ -> }
            .requiredBody<AccessTokenResponse>()

        AccessTokenResponseAssert.assertThat(response)
            .accessTokenIsNotNull()
            .tokenTypeIsBearer()
            .hasScopes(setOf(adminClient.scopes.first()))
            .expiresInIs299()
    }

    private fun scopes(scopes: List<String>): String {
        return scopes.joinToString(" ")
    }

    private fun basicAuth(username: String, password: String): String {
        val auth = "$username:$password"
        val encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.toByteArray())

        return "Basic $encodedAuth"
    }

    private data class AccessTokenResponse(
        @JsonAlias("access_token") val accessToken: String,
        @JsonAlias("token_type") val tokenType: String,
        @JsonAlias("expires_in") val expiresIn: Int,
        val scope: String
    ) {
        val scopes: Set<String>
            get() {
                return scope.split(" ").toSet()
            }
    }

    private class AccessTokenResponseAssert(actual: AccessTokenResponse) :
        AbstractAssert<AccessTokenResponseAssert, AccessTokenResponse>(actual, AccessTokenResponseAssert::class.java) {
        fun accessTokenIsNotNull(): AccessTokenResponseAssert {
            isNotNull()

            @Suppress("SENSELESS_COMPARISON")
            if (actual.accessToken == null) {
                failWithMessage("accessToken är null")
            }

            return this
        }

        fun tokenTypeIsBearer(): AccessTokenResponseAssert {
            isNotNull()

            if (actual.tokenType != "Bearer") {
                failWithMessage("tokenType är inte \"Bearer\" utan ${actual.tokenType}")
            }

            return this
        }

        fun hasScopes(scopes: Collection<String>): AccessTokenResponseAssert {
            isNotNull()

            assertThat(actual.scopes).containsExactlyInAnyOrderElementsOf(scopes)

            return this
        }

        fun expiresInIs299(): AccessTokenResponseAssert {
            isNotNull()

            if (actual.expiresIn != 299) {
                failWithMessage("expiresIn är inte 299 utan ${actual.expiresIn}")
            }

            return this
        }

        companion object {
            fun assertThat(actual: AccessTokenResponse): AccessTokenResponseAssert {
                return AccessTokenResponseAssert(actual)
            }
        }
    }
}

