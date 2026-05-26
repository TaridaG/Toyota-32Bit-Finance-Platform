package com.company.finance_api.news.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.finance_api.domain.NewsFavorite;
import com.company.finance_api.domain.User;
import com.company.finance_api.repository.NewsFavoriteRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsFavoriteServiceImplTest {

  @Mock NewsFavoriteRepository newsFavoriteRepository;
  @Mock UserRepository userRepository;
  @Mock CurrentUserResolver currentUserResolver;

  NewsFavoriteServiceImpl newsFavoriteService;

  final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    newsFavoriteService =
        new NewsFavoriteServiceImpl(newsFavoriteRepository, userRepository, currentUserResolver);
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
  }

  @Test
  void addFavorite_createsNewRow() {
    User user = mock(User.class);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(newsFavoriteRepository.findByUserIdAndNewsId(userId, 42L)).thenReturn(Optional.empty());

    newsFavoriteService.addFavorite(42L);

    verify(newsFavoriteRepository).save(any(NewsFavorite.class));
  }

  @Test
  void addFavorite_reactivatesInactiveRow() {
    User user = mock(User.class);
    NewsFavorite inactive = NewsFavorite.create(user, 7L);
    inactive.deactivate();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(newsFavoriteRepository.findByUserIdAndNewsId(userId, 7L))
        .thenReturn(Optional.of(inactive));

    newsFavoriteService.addFavorite(7L);

    verify(newsFavoriteRepository).save(inactive);
    assertEquals(true, inactive.isActive());
  }

  @Test
  void removeFavorite_deactivatesActiveRow() {
    User user = mock(User.class);
    NewsFavorite active = NewsFavorite.create(user, 9L);
    when(newsFavoriteRepository.findByUserIdAndNewsId(userId, 9L)).thenReturn(Optional.of(active));

    newsFavoriteService.removeFavorite(9L);

    verify(newsFavoriteRepository).save(active);
    assertEquals(false, active.isActive());
  }
}
