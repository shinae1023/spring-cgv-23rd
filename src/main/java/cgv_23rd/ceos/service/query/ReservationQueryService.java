package cgv_23rd.ceos.service.query;

import cgv_23rd.ceos.dto.reservation.response.ReservationResponseDto;
import cgv_23rd.ceos.mapper.ReservationMapper;
import cgv_23rd.ceos.repository.reservation.ReservationRepository;
import cgv_23rd.ceos.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;
    private final UserService userService;
    private final ReservationMapper reservationMapper;

    public List<ReservationResponseDto> getReservationList(Long userId) {
        userService.getUser(userId);

        return reservationRepository.findAllByUserIdWithDetails(userId).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }
}
