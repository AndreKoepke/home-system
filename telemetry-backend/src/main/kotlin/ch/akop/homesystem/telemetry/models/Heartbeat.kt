package ch.akop.homesystem.telemetry.models

import java.time.LocalDateTime
import java.util.*

data class Heartbeat(
        val id: UUID,
        val gitBranch: String,
        val gitCommit: String,
        val gitCommitDate: LocalDateTime
)
