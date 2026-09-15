package com.example.de_general

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform