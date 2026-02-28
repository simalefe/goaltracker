package com.goaltracker.service;

import com.goaltracker.dto.response.FriendRequestResponse;
import com.goaltracker.dto.response.FriendResponse;
import com.goaltracker.exception.ErrorCode;
import com.goaltracker.exception.FriendshipException;
import com.goaltracker.model.Friendship;
import com.goaltracker.model.User;
import com.goaltracker.model.enums.FriendshipStatus;
import com.goaltracker.repository.FriendshipRepository;
import com.goaltracker.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @InjectMocks
    private FriendshipService friendshipService;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private User requester;
    private User receiver;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setUsername("alice");
        requester.setDisplayName("Alice");
        requester.setEmail("alice@test.com");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("bob");
        receiver.setDisplayName("Bob");
        receiver.setEmail("bob@test.com");
    }

    @Nested
    @DisplayName("sendRequest")
    class SendRequestTests {

        @Test
        @DisplayName("Başarılı arkadaşlık isteği")
        void shouldSendRequest() {
            given(userRepository.findById(1L)).willReturn(Optional.of(requester));
            given(userRepository.findByUsername("bob")).willReturn(Optional.of(receiver));
            given(friendshipRepository.findBetweenUsers(1L, 2L)).willReturn(Optional.empty());
            given(friendshipRepository.save(any(Friendship.class))).willAnswer(inv -> {
                Friendship f = inv.getArgument(0);
                f.setId(100L);
                return f;
            });

            FriendRequestResponse response = friendshipService.sendRequest(1L, "bob");

            assertThat(response).isNotNull();
            assertThat(response.getRequesterId()).isEqualTo(1L);
            assertThat(response.getReceiverUsername()).isEqualTo("bob");
            assertThat(response.getStatus()).isEqualTo(FriendshipStatus.PENDING);

            then(notificationService).should().createNotification(eq(2L), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Kendine istek gönderme → hata")
        void shouldRejectSelfRequest() {
            given(userRepository.findById(1L)).willReturn(Optional.of(requester));
            given(userRepository.findByUsername("alice")).willReturn(Optional.of(requester));

            assertThatThrownBy(() -> friendshipService.sendRequest(1L, "alice"))
                    .isInstanceOf(FriendshipException.class)
                    .satisfies(ex -> assertThat(((FriendshipException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.SELF_FRIEND_REQUEST));
        }

        @Test
        @DisplayName("Zaten arkadaş → hata")
        void shouldRejectIfAlreadyFriends() {
            given(userRepository.findById(1L)).willReturn(Optional.of(requester));
            given(userRepository.findByUsername("bob")).willReturn(Optional.of(receiver));

            Friendship existing = new Friendship();
            existing.setStatus(FriendshipStatus.ACCEPTED);
            given(friendshipRepository.findBetweenUsers(1L, 2L)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> friendshipService.sendRequest(1L, "bob"))
                    .isInstanceOf(FriendshipException.class)
                    .satisfies(ex -> assertThat(((FriendshipException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_FRIENDS));
        }

        @Test
        @DisplayName("Bekleyen istek var → hata")
        void shouldRejectIfPendingExists() {
            given(userRepository.findById(1L)).willReturn(Optional.of(requester));
            given(userRepository.findByUsername("bob")).willReturn(Optional.of(receiver));

            Friendship existing = new Friendship();
            existing.setStatus(FriendshipStatus.PENDING);
            given(friendshipRepository.findBetweenUsers(1L, 2L)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> friendshipService.sendRequest(1L, "bob"))
                    .isInstanceOf(FriendshipException.class)
                    .satisfies(ex -> assertThat(((FriendshipException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.REQUEST_ALREADY_SENT));
        }

        @Test
        @DisplayName("Kullanıcı bulunamadı → hata")
        void shouldRejectIfReceiverNotFound() {
            given(userRepository.findById(1L)).willReturn(Optional.of(requester));
            given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() -> friendshipService.sendRequest(1L, "nonexistent"))
                    .isInstanceOf(FriendshipException.class);
        }
    }

    @Nested
    @DisplayName("acceptRequest")
    class AcceptRequestTests {

        @Test
        @DisplayName("Başarılı kabul")
        void shouldAcceptRequest() {
            Friendship friendship = new Friendship();
            friendship.setId(100L);
            friendship.setRequester(requester);
            friendship.setReceiver(receiver);
            friendship.setStatus(FriendshipStatus.PENDING);

            given(friendshipRepository.findById(100L)).willReturn(Optional.of(friendship));
            given(friendshipRepository.save(any(Friendship.class))).willAnswer(inv -> inv.getArgument(0));

            FriendRequestResponse response = friendshipService.acceptRequest(100L, 2L);

            assertThat(response.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
            then(notificationService).should().createNotification(eq(1L), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Sadece alıcı kabul edebilir")
        void shouldRejectIfNotReceiver() {
            Friendship friendship = new Friendship();
            friendship.setId(100L);
            friendship.setRequester(requester);
            friendship.setReceiver(receiver);
            friendship.setStatus(FriendshipStatus.PENDING);

            given(friendshipRepository.findById(100L)).willReturn(Optional.of(friendship));

            assertThatThrownBy(() -> friendshipService.acceptRequest(100L, 1L))
                    .isInstanceOf(FriendshipException.class);
        }
    }

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequestTests {

        @Test
        @DisplayName("Başarılı reddetme")
        void shouldRejectRequest() {
            Friendship friendship = new Friendship();
            friendship.setId(100L);
            friendship.setRequester(requester);
            friendship.setReceiver(receiver);
            friendship.setStatus(FriendshipStatus.PENDING);

            given(friendshipRepository.findById(100L)).willReturn(Optional.of(friendship));

            friendshipService.rejectRequest(100L, 2L);

            then(friendshipRepository).should().delete(friendship);
        }
    }

    @Nested
    @DisplayName("getFriends")
    class GetFriendsTests {

        @Test
        @DisplayName("Arkadaş listesi döner")
        void shouldReturnFriends() {
            Friendship f = new Friendship();
            f.setId(100L);
            f.setRequester(requester);
            f.setReceiver(receiver);
            f.setStatus(FriendshipStatus.ACCEPTED);

            given(friendshipRepository.findAcceptedFriends(1L)).willReturn(List.of(f));

            List<FriendResponse> friends = friendshipService.getFriends(1L);

            assertThat(friends).hasSize(1);
            assertThat(friends.get(0).getUsername()).isEqualTo("bob");
        }
    }

    @Nested
    @DisplayName("getPendingRequests")
    class GetPendingRequestsTests {

        @Test
        @DisplayName("Gelen ve giden istekleri döner")
        void shouldReturnPendingRequests() {
            Friendship incoming = new Friendship();
            incoming.setId(100L);
            incoming.setRequester(receiver);
            incoming.setReceiver(requester);
            incoming.setStatus(FriendshipStatus.PENDING);

            Friendship outgoing = new Friendship();
            outgoing.setId(101L);
            outgoing.setRequester(requester);
            outgoing.setReceiver(receiver);
            outgoing.setStatus(FriendshipStatus.PENDING);

            given(friendshipRepository.findPendingIncoming(1L)).willReturn(List.of(incoming));
            given(friendshipRepository.findPendingOutgoing(1L)).willReturn(List.of(outgoing));

            Map<String, List<FriendRequestResponse>> result = friendshipService.getPendingRequests(1L);

            assertThat(result.get("incoming")).hasSize(1);
            assertThat(result.get("outgoing")).hasSize(1);
        }
    }
}

