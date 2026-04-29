package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.like.TheaterLike;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.TheaterLikeRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class TheaterService {
    private final TheaterRepository theaterRepository;
    private final TheaterLikeRepository theaterLikeRepository;
    private final UserService userService;

    // 3. 영화관 찜
    @Transactional
    public void likeTheater(Long userId, Long theaterId) {
        User user = userService.getUser(userId);
        Theater theater = getTheater(theaterId);

        if (theaterLikeRepository.findByUserAndTheater(user, theater) != null) {
            return;
        }

        try {
            theaterLikeRepository.save(TheaterLike.of(user, theater));
        } catch (DataIntegrityViolationException e) {
            if (theaterLikeRepository.findByUserAndTheater(user, theater) != null) {
                return;
            }
            throw e;
        }
    }

    @Transactional
    public void unlikeTheater(Long userId, Long theaterId) {
        User user = userService.getUser(userId);
        Theater theater = getTheater(theaterId);

        TheaterLike theaterLike = theaterLikeRepository.findByUserAndTheater(user, theater);
        if (theaterLike != null) {
            theaterLikeRepository.delete(theaterLike);
        }
    }

    private Theater getTheater(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND, "영화관 조회 불가"));
    }

}
