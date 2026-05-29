package com.company.marketdataservice.catalog.registry.sync;

import com.company.marketdataservice.catalog.registry.AssetKind;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.catalog.registry.QuoteCurrency;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class InstrumentRegistryDbSyncTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private InstrumentRegistryDbSync sync;

    @BeforeEach
    void setUp() {
        sync = new InstrumentRegistryDbSync(jdbcTemplate, new SimpleMeterRegistry());
    }

    @Test
    void syncAll_skipsWhenNotPostgres() throws Exception {
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");

        sync.onApplicationReady();

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), any(), any(), any(), any());
    }

    @Test
    void syncOne_upsertsInstrumentCatalogAndCryptoMappings() throws Exception {
        IngestInstrumentDef crypto = new IngestInstrumentDef(
                "BTCUSDT",
                "BTCUSDT Crypto",
                AssetKind.CRYPTO,
                "BINANCE",
                QuoteCurrency.USDT,
                IngestProvider.COMPOSITE,
                "BTCUSDT",
                null,
                null,
                null
        );

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any(), any(), any()))
                .thenReturn(42L);

        sync.syncAll();

        verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), eq(Long.class), any(), any(), any(), any());
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(), any(), any(), any(), any());

        ArgumentCaptor<String> mappingSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(mappingSql.capture(), any(), any(), any(), any());
        assertTrue(mappingSql.getAllValues().stream()
                .anyMatch(sql -> sql.contains("mds_provider_instrument_mapping")));
    }
}
