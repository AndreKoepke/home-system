package ch.akop.homesystem.telemetry

import ch.akop.homesystem.telemetry.exceptions.TooManyRequests
import ch.akop.homesystem.telemetry.models.Heartbeat
import ch.akop.homesystem.telemetry.models.SyncAck
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


val templateText: String = object {}.javaClass.getResource("/badge/template.svg")!!.readText()

@Service
class Service(val bucketService: BucketService,
              val repository: Repository) {

    fun sync(identification: Any): SyncAck {
        if (!bucketService.obtainToken(identification)) {
            throw TooManyRequests(identification.toString())
        }

        val new = repository.save(TelemetryRecord(
                UUID.randomUUID(),
                lastContact = LocalDateTime.now(),
                null, null,
                null
        ))

        return SyncAck(new.id);
    }

    fun heartBeat(heartbeat: Heartbeat): Unit {
        repository.findById(heartbeat.id)
                .ifPresent {
                    it.contacts = if (Duration.between(it.lastContact, LocalDateTime.now()).toHours() > 10) it.contacts + 1 else it.contacts
                    it.lastContact = LocalDateTime.now()
                    it.gitBranch = heartbeat.gitBranch
                    it.gitCommit = heartbeat.gitCommit
                    it.gitCommitDate = heartbeat.gitCommitDate

                    repository.save(it)
                }
    }

    fun badge(): ByteArray {
        return templateText.replace("%KEY%", "Live")
                .replace("%VALUE%", repository.findAll().stream().filter { it.contacts > 3 && Duration.between(it.lastContact, LocalDateTime.now()).toDays() < 3 }.count().toString())
                .encodeToByteArray()
    }
}
