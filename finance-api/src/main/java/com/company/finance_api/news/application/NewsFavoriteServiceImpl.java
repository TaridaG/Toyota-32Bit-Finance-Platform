package com.company.finance_api.news.application;

import com.company.finance_api.news.domain.NewsFavorite;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.news.infrastructure.http.dto.NewsFavoriteItemDto;
import com.company.finance_api.news.infrastructure.persistence.NewsFavoriteRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** NewsFavoriteServiceImpl iş mantığını uygular (news favorite service). */
@Service
@Transactional
public class NewsFavoriteServiceImpl implements NewsFavoriteService {

  private final NewsFavoriteRepository newsFavoriteRepository;
  private final UserRepository userRepository;
  private final CurrentUserResolver currentUserResolver;

  public NewsFavoriteServiceImpl(
      NewsFavoriteRepository newsFavoriteRepository,
      UserRepository userRepository,
      CurrentUserResolver currentUserResolver) {
    this.newsFavoriteRepository = newsFavoriteRepository;
    this.userRepository = userRepository;
    this.currentUserResolver = currentUserResolver;
  }

  /** addFavorite işlemini gerçekleştirir. */
  @Override
  public void addFavorite(Long newsId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    NewsFavorite existing =
        newsFavoriteRepository.findByUserIdAndNewsId(userId, newsId).orElse(null);
    if (existing != null && existing.isActive()) {
      return;
    }

    NewsFavorite itemToSave;
    if (existing == null) {
      itemToSave = NewsFavorite.create(user, newsId);
    } else {
      existing.activate();
      itemToSave = existing;
    }
    newsFavoriteRepository.save(itemToSave);
  }

  /** removeFavorite işlemini uygular. */
  @Override
  public void removeFavorite(Long newsId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    NewsFavorite existing =
        newsFavoriteRepository.findByUserIdAndNewsId(userId, newsId).orElse(null);
    if (existing == null || !existing.isActive()) {
      return;
    }
    existing.deactivate();
    newsFavoriteRepository.save(existing);
  }

  @Override
  @Transactional(readOnly = true)
  /** MyFavorites sorgusunu döner. */
  public List<NewsFavoriteItemDto> getMyFavorites() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return newsFavoriteRepository.findByUserIdAndActiveTrue(userId).stream()
        .map(
            item -> new NewsFavoriteItemDto(item.getNewsId(), item.isActive(), item.getCreatedAt()))
        .toList();
  }
}
