package cgv_23rd.ceos.service;

import cgv_23rd.ceos.repository.ReservationRepository;
import cgv_23rd.ceos.repository.ReservationSeatRepositoy;
import cgv_23rd.ceos.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepositoy reservationSeatRepositoy;

    // 1. 영화 예매

    // 2. 영화 예매 취소

    // 3. 예매 내역 조회
}
