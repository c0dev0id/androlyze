package com.c0dev0id.androlyze.rules

/**
 * Represents a logcat analysis rule that can be enabled or disabled.
 */
data class Rule(
    val id: String,
    val name: String,
    val description: String,
    var isEnabled: Boolean = false
)
