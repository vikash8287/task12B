package com.company.chamberly.utils

enum class Role {
    VENTOR {
        override fun toString(): String {
            return "ventor"
        }
    },
    LISTENER {
        override fun toString(): String {
            return "listener"
        }
    }
}