package cgv_23rd.ceos.entity.movie;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MovieStatistics {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "movie_id", nullable = false, unique = true)
    private Movie movie;

    @Builder.Default
    private Integer audienceCount = 0;

    @Builder.Default
    private Double reservationRate = 0.0;

    @Builder.Default
    private Double maleRatio = 0.0;

    @Builder.Default
    private Double femaleRatio = 0.0;

    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Double eggRate = 0.0;

    public void addReviewRating(double newRating) {
        // 총점 계산
        double totalRating = this.averageRating * this.reviewCount;

        // 리뷰 수 증가 및 새로운 평균 계산
        this.reviewCount += 1;
        this.averageRating = (totalRating + newRating) / this.reviewCount;

        this.averageRating = Math.round(this.averageRating * 10.0) / 10.0;
    }

}
