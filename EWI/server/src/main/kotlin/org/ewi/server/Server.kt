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
        get("/") {
            call.respondText("EWIServer is running!", ContentType.Text.Plain)
        }
    }
}