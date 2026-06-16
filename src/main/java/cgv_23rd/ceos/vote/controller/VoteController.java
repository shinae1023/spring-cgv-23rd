package cgv_23rd.ceos.vote.controller;

import cgv_23rd.ceos.candidate.dto.response.CandidateResponseDto;
import cgv_23rd.ceos.user.entity.Part;
import cgv_23rd.ceos.vote.dto.request.PartVoteRequestDto;
import cgv_23rd.ceos.vote.dto.request.TeamVoteRequestDto;
import cgv_23rd.ceos.vote.dto.response.TeamVoteResultDto;
import cgv_23rd.ceos.vote.service.PartVoteService;
import cgv_23rd.ceos.vote.service.TeamVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Tag(name = "Vote", description = "투표 API")
public class VoteController {

    private final PartVoteService partVoteService;
    private final TeamVoteService teamVoteService;

    @Operation(summary = "파트장 투표", description = "로그인한 사용자가 자신의 파트와 같은 파트 후보자에게 1회 투표합니다.")
    @PostMapping("/part")
    public ResponseEntity<String> createPartVote(Authentication authentication,
                                                 @Valid @RequestBody PartVoteRequestDto requestDto) {
        return ResponseEntity.ok(partVoteService.createPartVote(extractUserId(authentication), requestDto));
    }

    @Operation(summary = "팀 투표", description = "로그인한 사용자가 자신의 팀을 제외한 다른 팀에 1회 투표합니다.")
    @PostMapping("/team")
    public ResponseEntity<String> createTeamVote(Authentication authentication,
                                                 @Valid @RequestBody TeamVoteRequestDto requestDto) {
        return ResponseEntity.ok(teamVoteService.createTeamVote(extractUserId(authentication), requestDto));
    }

    @Operation(summary = "팀 투표 결과 조회", description = "팀별 득표수를 내림차순으로 조회합니다. 득표가 없는 팀도 함께 반환합니다.")
    @GetMapping("/team")
    public ResponseEntity<List<TeamVoteResultDto>> getTeamVoteResults() {
        return ResponseEntity.ok(teamVoteService.getTeamVoteResults());
    }

    @Operation(summary = "파트별 후보 득표 조회", description = "선택한 파트 후보자들의 득표수를 내림차순으로 조회합니다.")
    @GetMapping("/part")
    public ResponseEntity<List<CandidateResponseDto>> getPartVoteResults(@RequestParam Part part) {
        return ResponseEntity.ok(partVoteService.getPartVoteResults(part));
    }

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalValue) {
            try {
                return Long.parseLong(principalValue);
            } catch (NumberFormatException ignored) {
            }
        }

        Object details = authentication.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }

        throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다.");
    }
}
