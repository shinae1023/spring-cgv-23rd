package cgv_23rd.ceos.service.lock;

import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class MySqlReservationNamedLockManager implements ReservationNamedLockManager {

    private static final String GET_LOCK_QUERY = "SELECT GET_LOCK(?, ?)";
    private static final String RELEASE_LOCK_QUERY = "SELECT RELEASE_LOCK(?)";
    private static final int LOCK_TIMEOUT_SECONDS = 3;

    private final DataSource dataSource;

    @Override
    public void acquireLocks(List<String> lockKeys) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Reservation named lock must be used inside a transaction.");
        }

        LockHolder holder = getOrCreateHolder();

        for (String lockKey : new LinkedHashSet<>(lockKeys)) {
            if (holder.lockKeys.contains(lockKey)) {
                continue;
            }
            tryAcquire(holder.connection, lockKey);
            holder.lockKeys.add(lockKey);
        }
    }

    private LockHolder getOrCreateHolder() {
        LockHolder existingHolder =
                (LockHolder) TransactionSynchronizationManager.getResource(MySqlReservationNamedLockManager.class);
        if (existingHolder != null) {
            return existingHolder;
        }

        try {
            Connection connection = dataSource.getConnection();
            LockHolder newHolder = new LockHolder(connection);
            TransactionSynchronizationManager.bindResource(MySqlReservationNamedLockManager.class, newHolder);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseAll(newHolder);
                    TransactionSynchronizationManager.unbindResource(MySqlReservationNamedLockManager.class);
                }
            });
            return newHolder;
        } catch (SQLException e) {
            throw new GeneralException(GeneralErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

    private void tryAcquire(Connection connection, String lockKey) {
        try (PreparedStatement statement = connection.prepareStatement(GET_LOCK_QUERY)) {
            statement.setString(1, lockKey);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new GeneralException(
                            GeneralErrorCode.RESERVATION_ALREADY_LOCKED
                    );
                }
            }
        } catch (SQLException e) {
            throw new GeneralException(GeneralErrorCode.LOCK_ACQUISITION_ERROR);
        }
    }

    private void releaseAll(LockHolder holder) {
        List<String> reversedKeys = new ArrayList<>(holder.lockKeys);
        for (int i = reversedKeys.size() - 1; i >= 0; i--) {
            release(holder.connection, reversedKeys.get(i));
        }

        try {
            holder.connection.close();
        } catch (SQLException ignored) {
        }
    }

    private void release(Connection connection, String lockKey) {
        try (PreparedStatement statement = connection.prepareStatement(RELEASE_LOCK_QUERY)) {
            statement.setString(1, lockKey);
            statement.executeQuery();
        } catch (SQLException ignored) {
        }
    }

    private static final class LockHolder {
        private final Connection connection;
        private final List<String> lockKeys = new ArrayList<>();

        private LockHolder(Connection connection) {
            this.connection = connection;
        }
    }
}
