package ch.akop.homesystem.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This class acts as a GateKeeper.
 */
public class TimedGateKeeper {

    private LocalDateTime blockedUntil;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Block for given duration. If the gateKeeper is already blocked,
     * then keep the blocker active, which wants a longer blocking-time.
     *
     * @param blockTime Duration of how long should the block be active.
     */
    public void blockFor(final Duration blockTime) {
        this.lock.writeLock().lock();

        final var newBlockedUntil = LocalDateTime.now().plus(blockTime);
        if (this.blockedUntil != null && this.blockedUntil.isAfter(newBlockedUntil)) {
            this.lock.writeLock().unlock();
            return;
        }

        this.blockedUntil = newBlockedUntil;
        this.lock.writeLock().unlock();
    }

    /**
     * Checks the current state the gate.
     *
     * @return true, when the gate is open
     */
    public boolean isGateOpen() {
        this.lock.readLock().lock();
        final var isGateOpen = this.blockedUntil == null || this.blockedUntil.isBefore(LocalDateTime.now());

        this.lock.readLock().unlock();
        return isGateOpen;
    }

    /**
     * Reset the lock.
     */
    public void reset() {
        this.lock.writeLock().lock();
        this.blockedUntil = null;
        this.lock.writeLock().unlock();
    }

}
