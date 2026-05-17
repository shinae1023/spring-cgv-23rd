package com.ceos.voteservice.vote.service;

import com.ceos.voteservice.candidate.dto.response.CandidateResponseDto;
import com.ceos.voteservice.candidate.entity.Candidate;
import com.ceos.voteservice.candidate.repository.CandidateRepository;
import com.ceos.voteservice.user.entity.Part;
import com.ceos.voteservice.user.entity.User;
import com.ceos.voteservice.user.repository.UserRepository;
import com.ceos.voteservice.vote.dto.request.PartVoteRequestDto;
import com.ceos.voteservice.vote.entity.PartVote;
import com.ceos.voteservice.vote.repository.PartVoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
