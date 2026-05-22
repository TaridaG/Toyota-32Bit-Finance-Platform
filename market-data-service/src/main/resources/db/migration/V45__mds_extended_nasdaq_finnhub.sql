-- Catalog + Finnhub mappings for extended NASDAQ tracked stocks (finance-api V50).
insert into mds_instrument_catalog (instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active)
select i.id, i.symbol, 'STOCK', null, 'USD', true
from instruments i
where i.symbol in ('TSLA', 'META', 'AVGO', 'AMD', 'NFLX', 'INTC', 'CSCO')
  and i.exchange = 'NASDAQ'
  and not exists (select 1 from mds_instrument_catalog c where c.instrument_id = i.id);

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'FINNHUB', i.symbol, i.id, 0, true
from instruments i
where i.symbol in ('TSLA', 'META', 'AVGO', 'AMD', 'NFLX', 'INTC', 'CSCO')
  and i.exchange = 'NASDAQ'
  and not exists (
    select 1
    from mds_provider_instrument_mapping m
    where m.provider = 'FINNHUB'
      and m.provider_symbol = i.symbol
  );
