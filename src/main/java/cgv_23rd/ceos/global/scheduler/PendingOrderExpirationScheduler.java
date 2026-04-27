package cgv_23rd.ceos.global.scheduler;

import cgv_23rd.ceos.service.PendingOrderExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderExpirationScheduler {

    private final PendingOrderExpirationService pendingOrderExpirationService;

    // 1분마다 만료 스캔
    @Scheduled(fixedDelay = 60000)
    public void expirePendingOrders() {
        pendingOrderExpirationService.expirePendingReservationsAndFoodOrders();
    }
}
