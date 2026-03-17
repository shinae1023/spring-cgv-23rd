package cgv_23rd.ceos.service;

import cgv_23rd.ceos.repository.TheaterRepository;
import cgv_23rd.ceos.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class TheaterService {
    private final TheaterRepository theaterRepository;
    private final UserRepository userRepository;

    // 1. 영화관 목록 조회

    // 2. 영화관 상세 조회

    // 3. 영화관 찜
}
