package com.goaltracker.service;

import com.goaltracker.dto.GoalResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.exception.ErrorCode;
import com.goaltracker.exception.FriendshipException;
import com.goaltracker.exception.GoalAccessDeniedException;
import com.goaltracker.exception.GoalNotFoundException;
import com.goaltracker.model.Goal;
import com.goaltracker.model.GoalShare;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.GoalStatus;
import com.goaltracker.model.enums.SharePermission;
import com.goaltracker.repository.GoalRepository;
import com.goaltracker.repository.GoalShareRepository;
import com.goaltracker.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GoalShareServiceTest {

    @InjectMocks
    private GoalShareService goalShareService;

    @Mock
    private GoalShareRepository goalShareRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipService friendshipService;

    @Mock
    private NotificationService notificationService;

    private User owner;
    private User target;
    private Goal goal;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");
        owner.setDisplayName("Alice");

        target = new User();
        target.setId(2L);
        target.setUsername("bob");
        target.setDisplayName("Bob");

        goal = new Goal();
        goal.setId(10L);
        goal.setUser(owner);
        goal.setTitle("Test Hedef");
        goal.setUnit("sayfa");
        goal.setTargetValue(new BigDecimal("100"));
        goal.setStartDate(LocalDate.now());
        goal.setEndDate(LocalDate.now().plusDays(30));
        goal.setStatus(GoalStatus.ACTIVE);
    }

    @Nested
    @DisplayName("shareGoal")
    class ShareGoalTests {

        @Test
        @DisplayName("Başarılı paylaşım")
        void shouldShareGoal() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(goal));
            given(friendshipService.areFriends(1L, 2L)).willReturn(true);
            given(goalShareRepository.existsByGoalIdAndSharedWithUserId(10L, 2L)).willReturn(false);
            given(userRepository.findById(2L)).willReturn(Optional.of(target));
            given(goalShareRepository.save(any(GoalShare.class))).willAnswer(inv -> inv.getArgument(0));

            goalShareService.shareGoal(10L, 1L, 2L, SharePermission.READ);

            then(goalShareRepository).should().save(any(GoalShare.class));
            then(notificationService).should().createNotification(eq(2L), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Hedef bulunamadı → hata")
        void shouldRejectIfGoalNotFound() {
            given(goalRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> goalShareService.shareGoal(999L, 1L, 2L, SharePermission.READ))
                    .isInstanceOf(GoalNotFoundException.class);
        }

        @Test
        @DisplayName("Sahiplik kontrolü → hata")
        void shouldRejectIfNotOwner() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(goal));

            assertThatThrownBy(() -> goalShareService.shareGoal(10L, 2L, 1L, SharePermission.READ))
                    .isInstanceOf(GoalAccessDeniedException.class);
        }

        @Test
        @DisplayName("Arkadaş değilse → hata")
        void shouldRejectIfNotFriends() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(goal));
            given(friendshipService.areFriends(1L, 2L)).willReturn(false);

            assertThatThrownBy(() -> goalShareService.shareGoal(10L, 1L, 2L, SharePermission.READ))
                    .isInstanceOf(FriendshipException.class)
                    .satisfies(ex -> assertThat(((FriendshipException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FRIENDS));
        }

        @Test
        @DisplayName("Zaten paylaşılmış → hata")
        void shouldRejectIfAlreadyShared() {
            given(goalRepository.findById(10L)).willReturn(Optional.of(goal));
            given(friendshipService.areFriends(1L, 2L)).willReturn(true);
            given(goalShareRepository.existsByGoalIdAndSharedWithUserId(10L, 2L)).willReturn(true);

            assertThatThrownBy(() -> goalShareService.shareGoal(10L, 1L, 2L, SharePermission.READ))
                    .isInstanceOf(FriendshipException.class)
                    .satisfies(ex -> assertThat(((FriendshipException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SHARE_ALREADY_EXISTS));
        }
    }

    @Nested
    @DisplayName("getSharedGoals")
    class GetSharedGoalsTests {

        @Test
        @DisplayName("Paylaşılan hedefler listesi")
        void shouldReturnSharedGoals() {
            GoalShare share = new GoalShare();
            share.setGoal(goal);
            share.setSharedWithUser(target);

            given(goalShareRepository.findBySharedWithUserId(2L)).willReturn(List.of(share));

            List<GoalResponse> result = goalShareService.getSharedGoals(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Test Hedef");
        }
    }

    @Nested
    @DisplayName("getSharedWithUsers")
    class GetSharedWithUsersTests {

        @Test
        @DisplayName("Paylaşılan kullanıcılar listesi")
        void shouldReturnSharedWithUsers() {
            GoalShare share = new GoalShare();
            share.setSharedWithUser(target);

            given(goalRepository.findById(10L)).willReturn(Optional.of(goal));
            given(goalShareRepository.findByGoalId(10L)).willReturn(List.of(share));

            List<FriendResponse> result = goalShareService.getSharedWithUsers(10L, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("bob");
        }
    }
}

