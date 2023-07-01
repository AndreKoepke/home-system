package ch.akop.homesystem.telemetry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.*


@Entity
data class TelemetryRecord(
        @Id var id: UUID,
        var lastContact: LocalDateTime,
        @Column(columnDefinition = "TEXT") var gitBranch: String?,
        @Column(columnDefinition = "TEXT") var gitCommit: String?,
        var gitCommitDate: LocalDateTime?,
        var contacts: Int = 0
)
