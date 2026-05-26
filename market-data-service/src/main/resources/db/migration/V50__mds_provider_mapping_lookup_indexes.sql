create index if not exists idx_mds_mapping_instrument_active_priority
    on mds_provider_instrument_mapping (instrument_id, active, priority);
