package cgv_23rd.ceos.controller.support;

import cgv_23rd.ceos.entity.user.User;
import cgv_23rd.ceos.entity.user.UserRole;
import cgv_23rd.ceos.global.security.UserDetailsImpl;
import cgv_23rd.ceos.service.AuthService;
import cgv_23rd.ceos.service.FoodOrderService;
import cgv_23rd.ceos.service.MovieService;
import cgv_23rd.ceos.service.ReservationService;
import cgv_23rd.ceos.service.ReviewService;
import cgv_23rd.ceos.service.TheaterService;
import cgv_23rd.ceos.service.admin.AdminFoodService;
import cgv_23rd.ceos.service.admin.AdminMovieService;
import cgv_23rd.ceos.service.pay.FoodPaymentFacade;
import cgv_23rd.ceos.service.pay.ReservationPaymentFacade;
import cgv_23rd.ceos.service.query.FoodOrderQueryService;
import cgv_23rd.ceos.service.query.MovieQueryService;
import cgv_23rd.ceos.service.query.ReservationQueryService;
import cgv_23rd.ceos.service.query.ReviewQueryService;
import cgv_23rd.ceos.service.query.ScheduleQueryService;
import cgv_23rd.ceos.service.query.TheaterQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected MovieService movieService;

    @MockitoBean
    protected MovieQueryService movieQueryService;

    @MockitoBean
    protected TheaterService theaterService;

    @MockitoBean
    protected TheaterQueryService theaterQueryService;

    @MockitoBean
    protected ReviewService reviewService;

    @MockitoBean
    protected ReviewQueryService reviewQueryService;

    @MockitoBean
    protected ReservationService reservationService;

    @MockitoBean
    protected ReservationQueryService reservationQueryService;

    @MockitoBean
    protected ReservationPaymentFacade reservationPaymentFacade;

    @MockitoBean
    protected FoodOrderService foodOrderService;

    @MockitoBean
    protected FoodOrderQueryService foodOrderQueryService;

    @MockitoBean
    protected FoodPaymentFacade foodPaymentFacade;

    @MockitoBean
    protected ScheduleQueryService scheduleQueryService;

    @MockitoBean
    protected AdminMovieService adminMovieService;

    @MockitoBean
    protected AdminFoodService adminFoodService;

    protected RequestPostProcessor authenticatedUser() {
        return SecurityMockMvcRequestPostProcessors.authentication(authentication(1L, UserRole.USER));
    }

    protected RequestPostProcessor authenticatedAdmin() {
        return SecurityMockMvcRequestPostProcessors.authentication(authentication(99L, UserRole.ADMIN));
    }

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Authentication authentication(Long userId, UserRole role) {
        User user = User.signup(
                "tester",
                "01012345678",
                LocalDate.of(2000, 1, 1),
                role.name().toLowerCase() + "@example.com",
                "encoded-password"
        );
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "role", role);

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
