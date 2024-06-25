package com.chamberly.chamberly.utils

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

enum class Entitlement {
    REGULAR,
    CHAMBERLY_PLUS
}