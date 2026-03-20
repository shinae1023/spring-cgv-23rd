package cgv_23rd.ceos.entity.theater;

import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.food.TheaterFood;
import cgv_23rd.ceos.entity.like.TheaterLike;
import cgv_23rd.ceos.entity.enums.Region;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;

    @Enumerated(EnumType.STRING)
    private Region region;

    private Boolean isAvailable;

    @Lob
    private String description;
    private String imageUrl;

    @OneToMany(mappedBy = "theater")
    private List<Screen> screens = new ArrayList<>();

    @OneToMany(mappedBy = "theater")
    private List<TheaterLike> theaterLikes = new ArrayList<>();

    @OneToMany(mappedBy = "theater")
    private List<TheaterFood> theaterFoods = new ArrayList<>();

    @OneToMany(mappedBy = "theater")
    private List<FoodOrder> foodOrders = new ArrayList<>();
}
