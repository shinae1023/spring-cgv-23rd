package cgv_23rd.ceos.entity.theater;

import cgv_23rd.ceos.entity.movie.MovieScreen;
import cgv_23rd.ceos.global.apiPayload.code.GeneralErrorCode;
import cgv_23rd.ceos.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_type_id", nullable = false)
    private ScreenType screenType;

    @Column(nullable = false)
    private String name;
    private Integer totalSeat;

    @OneToMany(mappedBy = "screen")
    private List<Seat> seats = new ArrayList<>();

    @OneToMany(mappedBy = "screen")
    private List<MovieScreen> movieScreens = new ArrayList<>();

    public void validateBelongsTo(Long theaterId) {
        if (!this.theater.getId().equals(theaterId)) {
            throw new GeneralException(GeneralErrorCode.SCREEN_THEATER_MISMATCH);
        }
    }
}
