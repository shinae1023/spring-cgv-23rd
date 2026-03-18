package cgv_23rd.ceos.service;

import cgv_23rd.ceos.repository.MovieRepository;
import cgv_23rd.ceos.repository.ReviewRepository;
import cgv_23rd.ceos.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

    // 1. 리뷰 생성


    // 2. 리뷰 조회
}
