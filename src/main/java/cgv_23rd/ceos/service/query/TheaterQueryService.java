package cgv_23rd.ceos.service.query;

import cgv_23rd.ceos.dto.theater.response.TheaterDetailResponseDto;
import cgv_23rd.ceos.dto.theater.response.TheaterResponseDto;
import cgv_23rd.ceos.entity.enums.Region;
import cgv_23rd.ceos.entity.theater.Theater;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import cgv_23rd.ceos.mapper.TheaterMapper;
import cgv_23rd.ceos.repository.theater.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TheaterQueryService {

    private final TheaterRepository theaterRepository;
    private final TheaterMapper theaterMapper;

    public List<TheaterResponseDto> getTheatersByRegion(Region region) {
        return theaterRepository.findAllByRegion(region).stream()
                .map(theaterMapper::toResponse)
                .toList();
    }

    public TheaterDetailResponseDto getTheaterDetail(Long theaterId) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.THEATER_NOT_FOUND, "극장 조회 불가"));
        return theaterMapper.toDetailResponse(theater);
    }
}
