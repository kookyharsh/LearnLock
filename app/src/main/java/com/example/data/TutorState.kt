package com.example.data

object TutorState {

    fun isActive(serviceEnabled: Boolean, disabledUntil: Long, now: Long): Boolean {
        if (!serviceEnabled) return false
        return disabledUntil <= 0L || now >= disabledUntil
    }

    fun isPaused(serviceEnabled: Boolean, disabledUntil: Long, now: Long): Boolean {
        return serviceEnabled && disabledUntil > 0L && now < disabledUntil
    }

    fun remainingMillis(disabledUntil: Long, now: Long): Long {
        return (disabledUntil - now).coerceAtLeast(0L)
    }
}
