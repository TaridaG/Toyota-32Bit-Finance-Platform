package com.company.finance_api.service.impl;

import com.company.finance_api.domain.NewsFavorite;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.NewsFavoriteItemDto;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.repository.NewsFavoriteRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.NewsFavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NewsFavoriteServiceImpl implements NewsFavoriteService {

    private final NewsFavoriteRepository newsFavoriteRepository;
    private final UserRepository userRepository;
    private final CurrentUserResolver currentUserResolver;

    public NewsFavoriteServiceImpl(
            NewsFavoriteRepository newsFavoriteRepository,
            UserRepository userRepository,
            CurrentUserResolver currentUserResolver
    ) {
        this.newsFavoriteRepository = newsFavoriteRepository;
        this.userRepository = userRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public void addFavorite(Long newsId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        NewsFavorite existing = newsFavoriteRepository.findByUserIdAndNewsId(userId, newsId).orElse(null);
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

    @Override
    public void removeFavorite(Long newsId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        NewsFavorite existing = newsFavoriteRepository.findByUserIdAndNewsId(userId, newsId).orElse(null);
        if (existing == null || !existing.isActive()) {
            return;
        }
        existing.deactivate();
        newsFavoriteRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsFavoriteItemDto> getMyFavorites() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return newsFavoriteRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(item -> new NewsFavoriteItemDto(item.getNewsId(), item.isActive(), item.getCreatedAt()))
                .toList();
    }
}
