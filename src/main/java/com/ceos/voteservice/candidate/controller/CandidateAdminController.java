package com.ceos.voteservice.candidate.controller;

import com.ceos.voteservice.candidate.dto.request.CandidateRequestDto;
import com.ceos.voteservice.candidate.dto.response.CandidateResponseDto;
import com.ceos.voteservice.candidate.service.CandidateAdminService;
import com.ceos.voteservice.user.entity.Part;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/admin/candidates")
@RestController
@Tag(name = "CandidateAdmin", description = "후보자 관리 API")
public class CandidateAdminController {
    private final CandidateAdminService candidateAdminService;

    @Operation(summary = "후보자 등록", description = "관리자가 새로운 후보자를 생성합니다. 후보자는 이름과 파트로 독립 생성됩니다.")
    @PostMapping
    public ResponseEntity<String> createCandidate(@Valid @RequestBody CandidateRequestDto dto) {
        return ResponseEntity.ok(candidateAdminService.createCandidate(dto));
    }

    @Operation(summary = "파트별 후보자 조회", description = "선택한 파트의 후보자 목록을 득표수 내림차순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CandidateResponseDto>> getCandidateList(@RequestParam Part part) {
        return ResponseEntity.ok(candidateAdminService.getCandidateList(part));
    }

    @Operation(summary = "후보자 삭제", description = "관리자가 후보자 아이디로 후보자를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<String> deleteCandidate(@RequestParam Long candidateId) {
        return ResponseEntity.ok(candidateAdminService.deleteCandidate(candidateId));
    }
}
