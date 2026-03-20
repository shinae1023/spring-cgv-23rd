package cgv_23rd.ceos.entity.user;

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
public class User {

    @Id @GeneratedValue
    private Long id;

    private String name;
    private String phone;
    private LocalDate birth;
    private String username;
    private String password;

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
