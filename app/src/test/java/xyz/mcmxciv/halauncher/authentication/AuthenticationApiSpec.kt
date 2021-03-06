package xyz.mcmxciv.halauncher.authentication

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import xyz.mcmxciv.halauncher.HalauncherMockWebServer
import xyz.mcmxciv.halauncher.authentication.models.Token

object AuthenticationApiSpec : Spek({
    val webServer by memoized { HalauncherMockWebServer(AuthenticationApi::class.java) }

    describe("Get Token") {
        context("on success") {
            lateinit var request: RecordedRequest
            var token: Any? = null

            beforeEachTest {
                webServer.enqueue(
                    200,
                    """{
                      "access_token": "ABCDEFGH",
                      "expires_in": 1800,
                      "refresh_token": "IJKLMNOPQRST",
                      "token_type": "Bearer"
                    }"""
                )
                token = runBlocking {
                    webServer.api.getToken(
                        AuthenticationRepository.GRANT_TYPE_CODE,
                        "12345",
                        AuthenticationRepository.CLIENT_ID
                    )
                }
                request = webServer.takeRequest()
            }

            it("should create a POST request") {
                assertThat(request.method).isEqualTo("POST")
                assertThat(request.path).isEqualTo("/auth/token")
                assertThat(request.body.readUtf8())
                    .contains("grant_type=authorization_code")
                    .contains("code=12345")
                    .contains("client_id=https%3A%2F%2Fhalauncher.app")
            }

            it("should return a token") {
                assertThat(token).isNotNull
                assertThat(token is Token).isTrue()
            }
        }
    }

    describe("Refresh Token") {
        context("on success") {
            lateinit var request: RecordedRequest
            var token: Any? = null

            beforeEachTest {
                webServer.enqueue(
                    200,
                    """{
                      "access_token": "ABCDEFGH",
                      "expires_in": 1800,
                      "token_type": "Bearer"
                    }"""
                )
                token = runBlocking {
                    webServer.api.refreshToken(
                        AuthenticationRepository.GRANT_TYPE_REFRESH,
                        "IJKLMNOPQRST",
                        AuthenticationRepository.CLIENT_ID
                    ).body()!!
                }
                request = webServer.takeRequest()
            }

            it("should create a POST request") {
                assertThat(request.method).isEqualTo("POST")
                assertThat(request.path).isEqualTo("/auth/token")
                assertThat(request.body.readUtf8())
                    .contains("grant_type=refresh_token")
                    .contains("refresh_token=IJKLMNOPQRST")
                    .contains("client_id=https%3A%2F%2Fhalauncher.app")
            }

            it("should return a token") {
                assertThat(token).isNotNull
                assertThat(token).isInstanceOf(Token::class.java)
            }
        }

        context("on error") {
            lateinit var request: RecordedRequest
            var errorBody: String? = null

            beforeEachTest {
                webServer.enqueue(
                    400,
                    """{
                      "error": "invalid_grant"
                    }"""
                )
                errorBody = runBlocking {
                    webServer.api.refreshToken(
                        AuthenticationRepository.GRANT_TYPE_REFRESH,
                        "IJKLMNOPQRST",
                        AuthenticationRepository.CLIENT_ID
                    ).errorBody()?.charStream()?.readText()
                }
                request = webServer.takeRequest()
            }

            it("should create a POST request") {
                assertThat(request.method).isEqualTo("POST")
                assertThat(request.path).isEqualTo("/auth/token")
                assertThat(request.body.readUtf8())
                    .contains("grant_type=refresh_token")
                    .contains("refresh_token=IJKLMNOPQRST")
                    .contains("client_id=https%3A%2F%2Fhalauncher.app")
            }

            it("should return an error") {
                assertThat(errorBody).isNotNull()
                assertThat(errorBody).contains(
                    """{
                      "error": "invalid_grant"
                    }"""
                )
            }
        }
    }

    describe("Revoke Token") {
        context("on success") {
            lateinit var request: RecordedRequest

            beforeEachTest {
                webServer.enqueue(200)
                runBlocking {
                    webServer.api.revokeToken(
                        "IJKLMNOPQRST",
                        AuthenticationRepository.REVOKE_ACTION
                    )
                }
                request = webServer.takeRequest()
            }

            it("should create a POST request") {
                assertThat(request.method).isEqualTo("POST")
                assertThat(request.path).isEqualTo("/auth/token")
                assertThat(request.body.readUtf8())
                    .contains("token=IJKLMNOPQRST")
                    .contains("action=revoke")
            }
        }
    }
})
