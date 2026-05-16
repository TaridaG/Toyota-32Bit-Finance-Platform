package com.company.finance_api.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chart_drawing_saves",
        indexes = {
                @Index(name = "idx_chart_drawing_saves_user_asset_created", columnList = "user_id, asset_key, created_at")
        }
)
public class ChartDrawingSave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @Column(name = "asset_key", nullable = false, length = 64)
    private String assetKey;

    @Column(name = "asset_symbol", nullable = false, length = 32)
    private String assetSymbol;

    @Column(name = "asset_type", length = 24)
    private String assetType;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "drawings_json", nullable = false, columnDefinition = "TEXT")
    private String drawingsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChartDrawingSave() {
    }

    public static ChartDrawingSave create(
            UUID userId,
            Instrument instrument,
            String assetKey,
            String assetSymbol,
            String assetType,
            String name,
            String drawingsJson
    ) {
        ChartDrawingSave row = new ChartDrawingSave();
        row.userId = userId;
        row.instrument = instrument;
        row.assetKey = assetKey;
        row.assetSymbol = assetSymbol;
        row.assetType = assetType;
        row.name = name.trim();
        row.drawingsJson = drawingsJson;
        Instant now = Instant.now();
        row.createdAt = now;
        row.updatedAt = now;
        return row;
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getName() {
        return name;
    }

    public String getDrawingsJson() {
        return drawingsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
