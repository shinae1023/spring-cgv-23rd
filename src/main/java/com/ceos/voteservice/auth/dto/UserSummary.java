package com.ceos.voteservice.auth.dto;

import com.ceos.voteservice.user.entity.Part;
import com.ceos.voteservice.user.entity.Team;
import com.ceos.voteservice.user.entity.User;

public record UserSummary(
        Long id,
        String loginId,
        String name,
        Part part,
        Team team
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getPart(),
                user.getTeam()
        );
    }
}
