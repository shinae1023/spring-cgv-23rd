package cgv_23rd.ceos.vote.service;

import cgv_23rd.ceos.candidate.dto.response.CandidateResponseDto;
import cgv_23rd.ceos.candidate.entity.Candidate;
import cgv_23rd.ceos.candidate.repository.CandidateRepository;
import cgv_23rd.ceos.user.entity.Part;
import cgv_23rd.ceos.user.entity.User;
import cgv_23rd.ceos.user.repository.UserRepository;
import cgv_23rd.ceos.vote.dto.request.PartVoteRequestDto;
import cgv_23rd.ceos.vote.entity.PartVote;
import cgv_23rd.ceos.vote.repository.PartVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartVoteService {
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final PartVoteRepository partVoteRepository;

    @Transactional
    public String createPartVote(Long userId, PartVoteRequestDto requestDto){
        User voter = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        if (partVoteRepository.existsByVoterId(userId)) {
            throw new IllegalArgumentException("파트장 투표는 1회만 가능합니다.");
        }

        Candidate candidate = candidateRepository.findById(requestDto.candidateId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후보자입니다."));

        partVoteRepository.save(PartVote.create(voter, candidate));
        return "파트장 투표가 완료되었습니다.";
    }

    @Transactional(readOnly = true)
    public List<CandidateResponseDto> getPartVoteResults(Part part) {
        return candidateRepository.findAllByPartOrderByVoteCountDesc(part);
    }
}
