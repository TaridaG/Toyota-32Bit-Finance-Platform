package com.company.finance_api.watchlist.infrastructure.http;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.dto.WatchlistItemDto;
import com.company.finance_api.service.WatchlistService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WatchlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class WatchlistControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private WatchlistService watchlistService;

  @Test
  void getMyWatchlist_returnsItems() throws Exception {
    WatchlistItemDto item =
        new WatchlistItemDto(1L, "BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, true, Instant.now());
    when(watchlistService.getMyWatchlist()).thenReturn(List.of(item));

    mockMvc
        .perform(get("/api/watchlist"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].symbol").value("BTCUSDT"))
        .andExpect(jsonPath("$.data[0].instrumentId").value(1));
  }

  @Test
  void addToWatchlist_delegatesToService() throws Exception {
    mockMvc
        .perform(
            post("/api/watchlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instrumentId\": 42}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(watchlistService).addToWatchlist(42L);
  }

  @Test
  void removeFromWatchlist_delegatesToService() throws Exception {
    mockMvc
        .perform(delete("/api/watchlist/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(watchlistService).removeFromWatchlist(eq(7L));
  }
}
