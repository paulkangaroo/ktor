/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.auth.providers

import io.ktor.client.features.auth.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.*

/**
 * Add [BasicAuthProvider] to client [Auth] providers.
 */
public fun Auth.bearer(block: BearerAuthConfig.() -> Unit) {
    with(BearerAuthConfig().apply(block)) {
        providers.add(BearerAuthProvider(refreshTokensFun, loadTokensFun, true, realm))
    }
}

public data class BearerTokens(
    val accessToken: String,
    val refreshToken: String
)

/**
 * [DigestAuthProvider] configuration.
 */
@Suppress("KDocMissingDocumentation")
public class BearerAuthConfig {
    public var refreshTokensFun: suspend () -> BearerTokens? = { null }
    public var loadTokensFun: suspend () -> BearerTokens? = { null }
    public var realm: String? = null
}

/**
 * Client digest [AuthProvider].
 */
@Suppress("KDocMissingDocumentation")
public class BearerAuthProvider(
    public val refreshTokensFun: suspend () -> BearerTokens?,
    public val loadTokensFun: suspend () -> BearerTokens?,
    override val sendWithoutRequest: Boolean = true,
    private val realm: String?
) : AuthProvider {

    private var cachedBearerTokens: BearerTokens? = null

    /**
     * Check if current provider is applicable to the request.
     */
    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        if (auth.authScheme != AuthScheme.Bearer) return false
        if (realm != null) {
            if (auth !is HttpAuthHeader.Parameterized) return false
            return auth.parameter("realm") == realm
        }
        return true
    }

    /**
     * Add authentication method headers and creds.
     */
    override suspend fun addRequestHeaders(request: HttpRequestBuilder) {
        val token = cachedBearerTokens ?: loadTokensFun()
        request.headers {
            token?.let {
                val tokenValue = "Bearer ${it.accessToken}"
                if(contains(HttpHeaders.Authorization)) {
                    remove(HttpHeaders.Authorization)
                }
                append(HttpHeaders.Authorization, tokenValue)
            }
        }
    }

    public suspend fun refreshToken(): BearerTokens? {
        cachedBearerTokens = refreshTokensFun()
        return cachedBearerTokens
    }

}
