package cgv_23rd.ceos.service.lock;

import java.util.List;

public interface ReservationNamedLockManager {

    void acquireLocks(List<String> lockKeys);
}
