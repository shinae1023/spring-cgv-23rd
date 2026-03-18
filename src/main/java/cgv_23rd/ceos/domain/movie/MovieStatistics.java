package cgv_23rd.ceos.domain.movie;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MovieStatistics {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "movie_id", nullable = false, unique = true)
    private Movie movie;

    private Integer audienceCount;
    private Double reservationRate;
    private Double maleRatio;
    private Double femaleRatio;
    private Double averageRating;
    private Integer reviewCount;
    private Double eggRate;

}
