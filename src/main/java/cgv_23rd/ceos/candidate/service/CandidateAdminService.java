package cgv_23rd.ceos.candidate.service;

import cgv_23rd.ceos.candidate.dto.request.CandidateRequestDto;
import cgv_23rd.ceos.candidate.dto.response.CandidateResponseDto;
import cgv_23rd.ceos.candidate.entity.Candidate;
import cgv_23rd.ceos.candidate.repository.CandidateRepository;
import cgv_23rd.ceos.user.entity.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateAdminService {

    private final CandidateRepository candidateRepository;

    @Transactional
    public String createCandidate(CandidateRequestDto requestDto) {
        Candidate candidate = Candidate.builder()
                .name(requestDto.name())
                .part(requestDto.part())
                .build();

        candidateRepository.save(candidate);
        return "후보자 등록이 완료되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<CandidateResponseDto> getCandidateList(Part part) {
        return candidateRepository.findAllByPartOrderByVoteCountDesc(part);
    }

    @Transactional
    public String deleteCandidate(Long candidateId) {
        candidateRepository.deleteById(candidateId);
        return "후보자 삭제가 완료되었습니다.";
    }
}
