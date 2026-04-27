package cgv_23rd.ceos.service;

import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.like.TheaterLike;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.TheaterLikeRepository;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import cgv_23rd.ceos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterService {
    private final TheaterRepository theaterRepository;
    private final UserRepository userRepository;
    private final TheaterLikeRepository theaterLikeRepository;

    // 1. 영화관 목록 조회 (지역 카테고리별)
    @Transactional(readOnly = true)
    public List<TheaterResponseDto> getTheatersByRegion(Region region){
        List<Theater> theaters = theaterRepository.findAllByRegion(region);

        return theaters.stream()
                .map(theater -> {
                    return TheaterResponseDto.builder()
                            .id(theater.getId())
                            .name(theater.getName())
                            .address(theater.getAddress())
                            .isActive(theater.getIsAvailable())
                            .build();
                })
                .toList();
    }

    // 2. 영화관 상세 조회
    @Transactional(readOnly = true)
    public TheaterDetailResponseDto getTheaterDetail(Long theaterId){
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND, "극장 조회 불가"));

        return TheaterDetailResponseDto.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .isActive(theater.getIsAvailable())
                .description(theater.getDescription())
                .imageUrl(theater.getImageUrl())
                .build();
    }

    // 3. 영화관 찜
    @Transactional
    public void likeTheater(Long userId, Long theaterId) {
        User user = getUser(userId);
        Theater theater = getTheater(theaterId);

        if (theaterLikeRepository.findByUserAndTheater(user, theater) != null) {
            return;
        }

        try {
            theaterLikeRepository.save(TheaterLike.of(user, theater));
        } catch (DataIntegrityViolationException e) {
            return;
        }
    }

    @Transactional
    public void unlikeTheater(Long userId, Long theaterId) {
        User user = getUser(userId);
        Theater theater = getTheater(theaterId);

        TheaterLike theaterLike = theaterLikeRepository.findByUserAndTheater(user, theater);
        if (theaterLike != null) {
            theaterLikeRepository.delete(theaterLike);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.USER_NOT_FOUND, "유저 조회 불가"));
    }

    private Theater getTheater(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND, "영화관 조회 불가"));
    }

}
