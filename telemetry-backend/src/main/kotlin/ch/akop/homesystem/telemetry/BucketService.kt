package ch.akop.homesystem.telemetry

import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped


const val BUCKET_CAPACITY = 10
const val IP_CAPACITY = 3

@ApplicationScoped
class BucketService {


    var freeToken: Int = BUCKET_CAPACITY
    val freeTokenPerIP: MutableMap<Any, Int> = HashMap()

    fun obtainToken(identification: Any): Boolean {
        if (freeTokenPerIP.computeIfAbsent(identification) { IP_CAPACITY } <= 0) {
            return false
        }

        if (freeToken <= 0) {
            return false
        }

        freeToken -= 1
        freeTokenPerIP[identification] = freeTokenPerIP[identification]!! - 1
        return true
    }


    @Scheduled(every = "10s")
    fun refillToken() {
        if (freeToken < BUCKET_CAPACITY) {
            freeToken += 1
        }

        freeTokenPerIP.forEach {
            if (it.value < IP_CAPACITY) {
                freeTokenPerIP[it.key] = it.value + 1
            }
        }
    }
}
