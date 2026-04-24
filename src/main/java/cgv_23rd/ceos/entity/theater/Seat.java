package cgv_23rd.ceos.entity.theater;

import cgv_23rd.ceos.entity.reservation.ReservationSeat;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_screen_row_col",
                        columnNames = {"screen_id", "row_name", "col_num"}
                )
        }
)
public class Seat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    private String rowName;
    private Integer colNum;
    private Boolean isActive;

    @OneToMany(mappedBy = "seat")
    private List<ReservationSeat> reservationSeats = new ArrayList<>();
}
