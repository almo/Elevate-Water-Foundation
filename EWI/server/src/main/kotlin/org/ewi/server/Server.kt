/*
 * 
 */
package org.ewi.server

import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.http.HttpStatusCode
import java.io.File
import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level
import org.ewi.core.HelloWorld

/**
 * The main object containing the entry point for the EWIServer application.
 */
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

/**
 * Configures the main application module, initializing monitoring and routing.
 */
fun Application.module() {
    configureMonitoring() 
    configureRouting()
}

/**
 * Configures monitoring and logging plugins for the application.
 */
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
    }
    EWIServer.logger.info("Logging setup completed")
}

/**
 * Configures the routing and available endpoints for the application.
 */
fun Application.configureRouting() {
    routing {
        
        // Keep your existing API routes
        get("/api/health") {
            call.respondText("EWIServer API is running!", ContentType.Text.Plain)
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

        // 2. Explicitly define your semantic UI routes FIRST
        get("/") {
            call.respondWithFragmentOrShell("main")
        }

        get("/settings") {
            call.respondWithFragmentOrShell("settings")
        }

        // 3. Fallback for all other static assets (JS, CSS, Vite bundles)
        // Notice we removed the `default("index.html")` because our explicit routes above handle that logic now.
        staticResources("/", "static")
    }
}