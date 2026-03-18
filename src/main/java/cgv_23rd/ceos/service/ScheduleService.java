package cgv_23rd.ceos.service;

import cgv_23rd.ceos.repository.MovieRepository;
import cgv_23rd.ceos.repository.TheaterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

    private final TheaterRepository theaterRepository;
    private final MovieRepository movieRepository;

    // 1. 극장별 상영 시간표 조회
}
