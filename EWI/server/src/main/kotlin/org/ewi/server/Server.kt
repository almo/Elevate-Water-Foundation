/*
 *
 */
package org.ewi.server

import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level

/** The main object containing the entry point for the EWIServer application. */
object EWIServer {
    /**
     * The main entry point of the application.
     *
     * @param args The command-line arguments.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // Starts the Ktor server on port 8080 using Netty
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
                .start(wait = true)
    }

    val logger = org.slf4j.LoggerFactory.getLogger(EWIServer::class.java)
}

/** Configures the main application module, initializing monitoring and routing. */
fun Application.module() {
    configureMonitoring()
    configureSecurity()
    configureRouting()
}

/** Configures monitoring and logging plugins for the application. */
fun Application.configureMonitoring() {
    install(CallLogging) { level = Level.INFO }
    EWIServer.logger.info("Logging setup completed")
}

/** Configures security and authentication for the application. */
fun Application.configureSecurity() {
    install(Authentication) {
        bearer("firebase") {
            authenticate { tokenCredential ->
                try {
                    // Verify the token sent from the frontend
                    val decodedToken =
                            FirebaseAuth.getInstance().verifyIdToken(tokenCredential.token)
                    UserIdPrincipal(decodedToken.uid)
                } catch (e: Exception) {
                    null // If verification fails, the user is unauthorized
                }
            }
        }
    }
}

/** Configures the routing and available endpoints for the application. */
fun Application.configureRouting() {
    routing {
        // 1. PUBLIC ROUTES: The shell and static assets must be accessible to everyone
        // so they can actually load the login page!
        get("/") { 
            // We only allow the Shell (index.html) to be public.
            // If it's an Alpine AJAX request for the 'main' fragment, 
            // we let the authenticated block below handle it.
            if (call.request.headers["X-Alpine-Request"] == "true") {
                call.respond(HttpStatusCode.Unauthorized, "Please login first.")
            } else {
                call.respondWithFragmentOrShell("main") 
            }
        }

        // Static files (JS/CSS) must be public
        staticResources("/", "static")

        get("/login") { call.respondWithFragmentOrShell("main") }

        // 2. PROTECTED ROUTES: Only accessible with a Firebase Token
        authenticate("firebase") {
            get("/api/health") {
                call.respondText("EWIServer API is running!", ContentType.Text.Plain)
            }

            // Fragment requests (X-Alpine-Request: true)
            get("/settings") { call.respondWithFragmentOrShell("settings") }
            
            // Re-define the root for AJAX only
            get("/main-fragment") { 
                // You might want to point your frontend to fetch /main-fragment 
                // instead of / for the initial data load
                call.respondWithFragmentOrShell("main") 
            }
        }
    }
}

// 1. Helper function to handle the Alpine AJAX logic using classpath resources
suspend fun ApplicationCall.respondWithFragmentOrShell(fragmentName: String) {
    val isAlpineRequest = request.headers["X-Alpine-Request"] == "true"

    if (isAlpineRequest) {
        // Serve the specific HTML snippet from the resources/static directory
        val fragmentResource = resolveResource("static/components/$fragmentName/template.html")

        if (fragmentResource != null) {
            respond(fragmentResource)
        } else {
            respond(HttpStatusCode.NotFound, "Fragment component not found: $fragmentName")
        }
    } else {
        // Serve the main index.html shell
        val shellResource = resolveResource("static/index.html")

        if (shellResource != null) {
            respond(shellResource)
        } else {
            respond(HttpStatusCode.InternalServerError, "Frontend build not found in resources.")
        }
    }
}
