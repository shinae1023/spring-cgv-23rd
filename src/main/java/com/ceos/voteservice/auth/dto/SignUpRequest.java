package com.ceos.voteservice.auth.dto;

import com.ceos.voteservice.user.entity.Part;
import com.ceos.voteservice.user.entity.Team;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(max = 30, message = "아이디는 30자 이하로 입력해주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
        String password,

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
        String email,

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 30, message = "이름은 30자 이하로 입력해주세요.")
        String name,

        @NotNull(message = "파트를 선택해주세요.")
        Part part,

        @NotNull(message = "팀을 선택해주세요.")
        Team team
) {
}
