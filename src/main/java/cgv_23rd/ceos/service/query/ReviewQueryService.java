package cgv_23rd.ceos.service.query;

import cgv_23rd.ceos.dto.review.response.ReviewResponseDto;
import cgv_23rd.ceos.mapper.ReviewMapper;
import cgv_23rd.ceos.repository.ReviewRepository;
import cgv_23rd.ceos.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final MovieService movieService;

    public List<ReviewResponseDto> getMovieReviews(Long movieId) {
        movieService.getMovie(movieId);

        return reviewRepository.findAllByMovieId(movieId).stream()
                .map(reviewMapper::toResponse)
                .toList();
    }
}
