package com.example

import config.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import plugins.configureRouting
import plugins.configureSecurity
import plugins.configureSerialization
import services.AuthService
import services.EventService
import services.UserService

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Database Initiation
    DatabaseFactory.init()

    // Service Layer Mapping
    val userService = UserService()
    val authService = AuthService(userService)
    val eventService = EventService()

    // Config Plugins
    configureSecurity()
    configureSerialization()
    configureRouting(authService, userService, eventService)
}