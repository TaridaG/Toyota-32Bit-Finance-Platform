package com.company.finance_api.infocards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.InfoCardEntity;
import com.company.finance_api.infocards.infrastructure.http.dto.InfoCardInputDto;
import com.company.finance_api.repository.InfoCardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InfoCardServiceTest {

  @Mock InfoCardRepository repository;

  InfoCardService service;

  @BeforeEach
  void setUp() {
    service = new InfoCardService(repository, new ObjectMapper());
  }

  @Test
  void dashboard_countsActiveAndPassiveCards() {
    InfoCardEntity active = withId(sampleCard("ACTIVE"));
    InfoCardEntity passive = withId(sampleCard("PASSIVE"));
    when(repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")))
        .thenReturn(List.of(active, passive));

    var dashboard = service.dashboard();

    assertThat(dashboard.activeCards()).isEqualTo(1);
    assertThat(dashboard.passiveCards()).isEqualTo(1);
  }

  @Test
  void toggleStatus_flipsBetweenActiveAndPassive() {
    InfoCardEntity card = withId(sampleCard("ACTIVE"));
    UUID id = card.getId();
    when(repository.findById(id)).thenReturn(Optional.of(card));
    when(repository.save(card)).thenReturn(card);

    var dto = service.toggleStatus(id);

    assertThat(dto.status()).isEqualTo("PASSIVE");
    verify(repository).save(card);
  }

  @Test
  void delete_throwsWhenCardMissing() {
    UUID id = UUID.randomUUID();
    when(repository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(id))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void seedDefaults_returnsZeroWhenDataAlreadyExists() {
    when(repository.count()).thenReturn(3L);

    assertThat(service.seedDefaults()).isZero();
    verify(repository, never()).save(any());
  }

  @Test
  void listPortalCards_filtersByPageKey() {
    InfoCardEntity dashboardCard = withId(sampleCard("ACTIVE"));
    dashboardCard.setPages(List.of("DASHBOARD"));
    InfoCardEntity other = withId(sampleCard("ACTIVE"));
    other.setPages(List.of("PORTFOLIO"));
    when(repository.findByStatusOrderByUpdatedAtDesc("ACTIVE"))
        .thenReturn(List.of(dashboardCard, other));

    var cards = service.listPortalCards("DASHBOARD", false, "tr");

    assertThat(cards).hasSize(1);
    assertThat(cards.getFirst().pages()).contains("DASHBOARD");
  }

  private static InfoCardEntity sampleCard(String status) {
    InfoCardInputDto input =
        new InfoCardInputDto(
            null,
            "sample-card",
            "Sample title",
            List.of("term"),
            List.of(),
            List.of(),
            List.of("DASHBOARD"),
            "GENERAL",
            "TERM",
            "BEGINNER",
            status,
            "Short description text",
            "Detailed description",
            null,
            null,
            null,
            List.of(),
            false,
            null);
    return InfoCardMapper.newEntity(input);
  }

  private static InfoCardEntity withId(InfoCardEntity entity) {
    entity.setId(UUID.randomUUID());
    return entity;
  }
}
