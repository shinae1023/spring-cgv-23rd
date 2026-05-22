package cgv_23rd.ceos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingOrderExpirationService {

    private final PendingReservationExpirationService pendingReservationExpirationService;
    private final PendingFoodOrderExpirationService pendingFoodOrderExpirationService;

    public void expirePendingReservationsAndFoodOrders() {
        expirePendingReservations();
        expirePendingFoodOrders();
    }

    public int expirePendingReservations() {
        return pendingReservationExpirationService.expirePendingReservations();
    }

    public int expirePendingFoodOrders() {
        return pendingFoodOrderExpirationService.expirePendingFoodOrders();
    }
}
