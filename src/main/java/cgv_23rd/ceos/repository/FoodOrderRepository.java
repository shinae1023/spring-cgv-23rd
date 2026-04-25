package cgv_23rd.ceos.repository;

import cgv_23rd.ceos.entity.enums.FoodOrderStatus;
import cgv_23rd.ceos.entity.food.FoodOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fo from FoodOrder fo where fo.id = :orderId")
    Optional<FoodOrder> findByIdWithLock(@Param("orderId") Long orderId);

    @Query("SELECT DISTINCT fo FROM FoodOrder fo " +
            "JOIN FETCH fo.theater t " +
            "LEFT JOIN FETCH fo.foodOrderItems foi " +
            "LEFT JOIN FETCH foi.food f " +
            "WHERE fo.user.id = :userId")
    List<FoodOrder> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update FoodOrder fo
           set fo.status = :canceledStatus
         where fo.status = :pendingStatus
           and fo.createdAt < :expiredAt
    """)
    int expirePendingFoodOrders(
            @Param("pendingStatus") FoodOrderStatus pendingStatus,
            @Param("canceledStatus") FoodOrderStatus canceledStatus,
            @Param("expiredAt") LocalDateTime expiredAt
    );

}
