package com.nzelot.filebase.data.model

enum class StatusLogSeverity {
    ERROR {
        override fun short(): String = "E"
    },
    INFO {
        override fun short(): String = "I"
    };

    abstract fun short(): String
}