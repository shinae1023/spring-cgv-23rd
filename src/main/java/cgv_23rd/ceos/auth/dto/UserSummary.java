package cgv_23rd.ceos.auth.dto;

import cgv_23rd.ceos.user.entity.Part;
import cgv_23rd.ceos.user.entity.Team;
import cgv_23rd.ceos.user.entity.User;

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
