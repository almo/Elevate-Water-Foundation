/*
 * Main Server Configuration for the Elevate Water Index (EWI) platform.
 * This file configures Ktor plugins, routing, security, and static asset serving.
 */
package org.ewi.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.http.content.CachingOptions
import io.ktor.server.http.content.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.compression.zstd.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.event.Level
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.cachingheaders.*

/** The main object containing the entry point for the EWIServer application. */
object EWIServer {
    /**
     * The main entry point of the application.
     *
     * @param args The command-line arguments.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // Starts the Ktor server on port 8080 using CIO
        // The `wait = true` parameter keeps the server running indefinitely
        embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
                .start(wait = true)
    }

    val logger = org.slf4j.LoggerFactory.getLogger(EWIServer::class.java)
}

/** Configures the main application module, initializing monitoring and routing. */
fun Application.module() {
    configureMonitoring()
    configureFirebase()
    configureSecurity()
    configureCompression()
    configureRouting()
}

/** Configures monitoring and logging plugins for the application. */
fun Application.configureMonitoring() {
    install(CallLogging) { level = Level.INFO }
    EWIServer.logger.info("Logging setup completed")
}

/** Configures the Firebase Admin SDK. */
fun Application.configureFirebase() {
    if (FirebaseApp.getApps().isEmpty()) {
        val options =
                FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId("elevate-water-foundation")
                        .build()
        FirebaseApp.initializeApp(options)
        EWIServer.logger.info("Firebase initialized successfully")
    }
}

/** Configures security and authentication for the application. */
fun Application.configureSecurity() {
    install(Authentication) {
        // Configures the "firebase" bearer authentication provider
        bearer("firebase") {
            authenticate { tokenCredential ->
                // Offload blocking network/crypto work to the IO dispatcher
                // This prevents thread starvation on Ktor's main event loop
                withContext(Dispatchers.IO) {
                    try {
                        // Verify the token sent from the frontend
                        val decodedToken =
                                FirebaseAuth.getInstance().verifyIdToken(tokenCredential.token)
                        // If successful, return the principal containing the user's UID
                        UserIdPrincipal(decodedToken.uid)
                    } catch (e: Exception) {
                        EWIServer.logger.error("Firebase token verification failed", e)
                        null // If verification fails, the user is unauthorized
                    }
                }
            }
        }
    }

    // Enables ETag and Last-Modified support to help the client cache static files
    install(ConditionalHeaders) // Enables ETag/Last-Modified support
}

/** Configures compression for the application. */
fun Application.configureCompression() {
    install(Compression) {
        // 1. Enable algorithms and set priorities (higher number = higher priority)
        zstd { priority = 1.1 }
        gzip { priority = 1.0 }
        deflate { priority = 0.9 }

        // Only compress payloads larger than 1KB to avoid compression overhead on tiny responses
        minimumSize(1024)
        matchContentType(
                ContentType.Application.Json,
                ContentType.Text.Html,
                ContentType.Text.CSS,
                ContentType.Application.JavaScript
        )

        // HTTPS Security: Mitigate BREACH attacks (Browser Reconnaissance and Exfiltration via Adaptive Compression of Hypertext)
        condition {
            // Only compress if the request originates from our own domain.
            // This prevents attackers from using compressed payload sizes to guess secrets.
            request.headers[HttpHeaders.Referrer]?.startsWith(
                    "https://elevate-water-foundation.oa.r.appspot.com"
            ) == true
        }
    }
}

/** 
 TODO:
 1) Versioning: Ensure your build pipeline adds hashes to filenames 
 (e.g., style.v2.css). This allows you to set max-age to one year safely.
 2) Public vs Private: If any fragment contains user-specific data
 (like /settings), ensure the header is CacheControl.Private.
*/
fun Application.configureCaching() {
    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                // 1. Static Assets (JS, CSS, Images)
                // Use "immutable" for files with hashes in names (e.g., app.8f2d1.js)
                ContentType.Application.JavaScript, 
                ContentType.Text.CSS -> 
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 31536000)) // 1 year

                // 2. HTML Templates/Fragments
                // We want these cached briefly or validated via ETag
                ContentType.Text.Html -> 
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600)) // 1 hour
                
                else -> null
            }
        }
    }
}

fun Application.configureRouting() {
    routing {
        // Serve static frontend files (CSS, JS, images) from the root path
        // Reverted to root to match frontend asset expectations (e.g., <script src="/app.js">)
        staticResources("/", "static")

        // Public routes - accessible without a valid Firebase token
        authenticate("firebase", optional = true) {
            // The root and login routes load the main application shell
            get("/") { call.handleHtmxOrAlpineRequest("main") }
            get("/login") { call.handleHtmxOrAlpineRequest("main") }
        }

        // Protected routes - require a valid Firebase Bearer token
        authenticate("firebase") {
            route("/api") {
                get("/health") { call.respond(HttpStatusCode.OK, "EWIServer API is running!") }
            }
            
            // Fragment routes requested by the Alpine AJAX frontend
            get("/settings") { call.handleHtmxOrAlpineRequest("settings") }
            get("/main-fragment") { call.handleHtmxOrAlpineRequest("main") }
        }
    }
}

/**
 * Optimized helper for SPA/MPA hybrid loading.
 * Serves either an HTML fragment (for Alpine AJAX requests) or the full index.html shell.
 * Now correctly uses resolveResource and extends ApplicationCall.
 * 
 * @param fragmentName The name of the component directory to serve if it's a fragment request.
 */
suspend fun ApplicationCall.handleHtmxOrAlpineRequest(fragmentName: String) {
    // Check if the request is an Alpine AJAX request asking for a partial HTML fragment
    val isFragment = request.headers["X-Alpine-Request"] == "true"
    
    // Determine the resource path based on the header
    val resourcePath = if (isFragment) {
        "static/components/$fragmentName/template.html"
    } else {
        "static/index.html"
    }

    // Attempt to locate the file in the classpath resources
    val resource = resolveResource(resourcePath)

    if (resource != null) {
        respond(resource)
    } else {
        // Fallback or Error handling when the file is missing
        if (isFragment) {
            respond(HttpStatusCode.NotFound, "Fragment $fragmentName not found.")
        } else {
            respond(HttpStatusCode.InternalServerError, "Critical: index.html missing from resources.")
        }
    }
}
