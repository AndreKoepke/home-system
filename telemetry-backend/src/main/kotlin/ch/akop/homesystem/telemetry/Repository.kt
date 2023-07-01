package ch.akop.homesystem.telemetry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface Repository : JpaRepository<TelemetryRecord, UUID>
