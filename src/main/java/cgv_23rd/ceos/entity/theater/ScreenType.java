package cgv_23rd.ceos.entity.theater;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScreenType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String typeName;
    private Integer basePrice;

    @OneToMany(mappedBy = "screenType")
    private List<Screen> screens = new ArrayList<>();

    @OneToMany(mappedBy = "screenType")
    private List<SeatTemplate> seatTemplates = new ArrayList<>();
}