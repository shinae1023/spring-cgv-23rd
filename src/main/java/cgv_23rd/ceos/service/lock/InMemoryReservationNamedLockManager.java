package cgv_23rd.ceos.service.lock;

import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Profile("test")
public class InMemoryReservationNamedLockManager implements ReservationNamedLockManager {

    private static final long LOCK_TIMEOUT_MILLIS = 3_000L;

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public void acquireLocks(List<String> lockKeys) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Reservation named lock must be used inside a transaction.");
        }

        LockHolder holder =
                (LockHolder) TransactionSynchronizationManager.getResource(InMemoryReservationNamedLockManager.class);
        if (holder == null) {
            holder = new LockHolder();
            TransactionSynchronizationManager.bindResource(InMemoryReservationNamedLockManager.class, holder);
            LockHolder finalHolder = holder;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseAll(finalHolder);
                    TransactionSynchronizationManager.unbindResource(InMemoryReservationNamedLockManager.class);
                }
            });
        }

        for (String lockKey : new LinkedHashSet<>(lockKeys)) {
            if (holder.lockKeys.contains(lockKey)) {
                continue;
            }

            ReentrantLock lock = locks.computeIfAbsent(lockKey, key -> new ReentrantLock());
            boolean acquired;
            try {
                acquired = lock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR, "예매 락 획득이 중단되었습니다.");
            }

            if (!acquired) {
                throw new GeneralException(
                        GeneralErrorCode.RESERVATION_ALREADY_LOCKED);
            }

            holder.lockKeys.add(lockKey);
            holder.acquiredLocks.add(lock);
        }
    }

    private void releaseAll(LockHolder holder) {
        for (int i = holder.acquiredLocks.size() - 1; i >= 0; i--) {
            holder.acquiredLocks.get(i).unlock();
        }
    }

    private static final class LockHolder {
        private final List<String> lockKeys = new ArrayList<>();
        private final List<ReentrantLock> acquiredLocks = new ArrayList<>();
    }
}
