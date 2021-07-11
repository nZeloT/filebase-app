package com.nzelot.filebase.ui.server_config

data class ConfigState(
    val isTestable: Boolean = false,
    val isSuccessfullyTested: Boolean = false,
    val isTestOngoing: Boolean = false,
    val isTestCompleted: Boolean = false,
    val testResultMessage: String = "",
    val testResultExplanation: String = "",
)
