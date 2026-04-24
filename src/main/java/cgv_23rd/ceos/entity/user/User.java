package cgv_23rd.ceos.entity.user;

import cgv_23rd.ceos.entity.BaseEntity;
import cgv_23rd.ceos.entity.food.FoodOrder;
import cgv_23rd.ceos.entity.like.MovieLike;
import cgv_23rd.ceos.entity.like.TheaterLike;
import cgv_23rd.ceos.entity.movie.Review;
import cgv_23rd.ceos.entity.reservation.Reservation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<MovieLike> movieLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<TheaterLike> theaterLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<FoodOrder> foodOrders = new ArrayList<>();
}
