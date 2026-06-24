package com.example

import config.DatabaseFactory
import config.databaseSeeder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import configureRouting
import plugins.configureCORS
import plugins.configureSecurity
import plugins.configureSerialization
import services.AuthService
import services.EventService
import services.UserService
import utils.Env

fun main(args: Array<String>) {
    try {
        println("Starting ProjectStore Server...")
        println("Port: ${Env.PORT}")
        println("Environment variables loaded")

        embeddedServer(Netty, port = Env.PORT, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    } catch (e: Exception) {
        println("FATAL ERROR: Application failed to start")
        println("Error: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

fun Application.module() {

    try {
        // Initialize database
        println("Initializing database connection...")
        println("Database URL: ${Env.DB_URL.take(50)}...") // Log partial URL for debugging
        println("Database Type: mysql")
        println("Database User: ${Env.DB_USER}")

        DatabaseFactory.init(Env.DB_URL, Env.DB_USER, Env.DB_PASSWORD, "mysql")
        println("Database initialized successfully")
    } catch (e: Exception) {
        println("ERROR: Failed to initialize database: ${e.message}")
        e.printStackTrace()
        throw e // Re-throw to prevent app from starting with broken DB
    }

    databaseSeeder()

    try {
        // Service Layer Mapping
        val userService = UserService()
        val authService = AuthService(userService)
        val eventService = EventService()

        // Config Plugins
        configureSecurity()
        configureCORS()
        configureSerialization()
        configureRouting(authService, userService, eventService)
        println("Plugins configured successfully")

    } catch (e: Exception) {
        println("ERROR: Failed to configure plugins: ${e.message}")
        e.printStackTrace()
        throw e
    }

    println("Application started successfully on port ${Env.PORT}")

}