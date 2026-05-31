package com.company.finance_api.chart.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.company.finance_api.chart.infrastructure.persistence.ChartDrawingSaveRepository;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChartDrawingSaveServiceImplTest {

  @Mock private ChartDrawingSaveRepository chartDrawingSaveRepository;
  @Mock private InstrumentRepository instrumentRepository;
  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private ChartDrawingSaveServiceImpl service;

  @Test
  void getById_shouldThrowNotFound_whenMissing() {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(chartDrawingSaveRepository.findByIdAndUserId(404L, userId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getById(404L));
  }
}
