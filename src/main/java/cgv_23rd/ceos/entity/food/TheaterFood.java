package cgv_23rd.ceos.entity.food;

import cgv_23rd.ceos.entity.theater.Theater;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_theater_food", columnNames = {"theater_id", "food_id"})
        }
)
public class TheaterFood {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    private Integer amount;

    public void decreaseAmount(){
        if(this.amount > 0){
            this.amount--;
        }
    }
}
