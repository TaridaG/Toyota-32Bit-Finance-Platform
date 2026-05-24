package com.company.finance_api.service.impl;

import com.company.finance_api.domain.NewsFavorite;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.NewsFavoriteItemDto;
import com.company.finance_api.repository.NewsFavoriteRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.NewsFavoriteService;
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
