package cgv_23rd.ceos.service;

import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.like.TheaterLike;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.dto.theater.request.TheaterRequestDto;
import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.global.apiPayload.ApiResponse;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.repository.TheaterLikeRepository;
import cgv_23rd.ceos.repository.TheaterRepository;
import cgv_23rd.ceos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    public ApiResponse<List<TheaterResponseDto>> getTheatersByRegion(Region region){
        List<Theater> theaters = theaterRepository.findAllByRegion(region);

        List<TheaterResponseDto> responseDtos = theaters.stream()
                .map(theater -> {
                    return TheaterResponseDto.builder()
                            .id(theater.getId())
                            .name(theater.getName())
                            .address(theater.getAddress())
                            .isActive(theater.getIsAvailable())
                            .build();
                })
                .toList();
        return ApiResponse.onSuccess("지역별 영화관 리스트 조회 성공", responseDtos);
    }

    // 2. 영화관 상세 조회
    @Transactional(readOnly = true)
    public ApiResponse<TheaterDetailResponseDto> getTheaterDetail(Long theaterId){
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND, "극장 조회 불가"));

        TheaterDetailResponseDto responseDto = TheaterDetailResponseDto.builder()
                .id(theater.getId())
                .name(theater.getName())
                .address(theater.getAddress())
                .isActive(theater.getIsAvailable())
                .description(theater.getDescription())
                .imageUrl(theater.getImageUrl())
                .build();

        return ApiResponse.onSuccess("영화관 상세 조회 성공", responseDto);
    }

    // 3. 영화관 찜
    @Transactional
    public ApiResponse<Void> toggleTheaterLike(Long userId, Long theaterId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.USER_NOT_FOUND,"유저 조회 불가"));

        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(()-> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND,"영화관 조회 불가"));

        TheaterLike theaterLike = theaterLikeRepository.findByUserAndTheater(user, theater);

        if(theaterLike==null){
            theaterLike = TheaterLike.builder()
                    .user(user)
                    .theater(theater)
                    .build();
            theaterLikeRepository.save(theaterLike);
            return ApiResponse.onSuccess("영화관 id = " + theaterId + " 찜 성공");
        }
        else{
            theaterLikeRepository.delete(theaterLike);
            return ApiResponse.onSuccess("영화관 id = " + theaterId + " 찜 삭제 성공");
        }
    }

    // 4. 극장 생성
    @Transactional
    public ApiResponse<Void> createTheater(TheaterRequestDto requestDto){
        Theater theater = Theater.builder()
                .name(requestDto.name())
                .address(requestDto.address())
                .region(requestDto.region())
                .isAvailable(true)
                .description(requestDto.description())
                .imageUrl(requestDto.imageUrl())
                .build();

        theaterRepository.save(theater);

        return ApiResponse.onSuccess("극장 생성 완료 id = " + theater.getId());
    }
}
