insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'FINNHUB', 'AAPL', i.id, 0, true
from instruments i
where i.symbol = 'AAPL'
  and i.type = 'STOCK'
  and not exists (
    select 1 from mds_provider_instrument_mapping m
    where m.provider = 'FINNHUB'
      and m.provider_symbol = 'AAPL'
  );

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'FINNHUB', 'AMZN', i.id, 0, true
from instruments i
where i.symbol = 'AMZN'
  and i.type = 'STOCK'
  and not exists (
    select 1 from mds_provider_instrument_mapping m
    where m.provider = 'FINNHUB'
      and m.provider_symbol = 'AMZN'
  );

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'FINNHUB', 'NVDA', i.id, 0, true
from instruments i
where i.symbol = 'NVDA'
  and i.type = 'STOCK'
  and not exists (
    select 1 from mds_provider_instrument_mapping m
    where m.provider = 'FINNHUB'
      and m.provider_symbol = 'NVDA'
  );

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'FINNHUB', 'MSFT', i.id, 0, true
from instruments i
where i.symbol = 'MSFT'
  and i.type = 'STOCK'
  and not exists (
    select 1 from mds_provider_instrument_mapping m
    where m.provider = 'FINNHUB'
      and m.provider_symbol = 'MSFT'
  );

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'FINNHUB', 'GOOGL', i.id, 0, true
from instruments i
where i.symbol = 'GOOGL'
  and i.type = 'STOCK'
  and not exists (
    select 1 from mds_provider_instrument_mapping m
    where m.provider = 'FINNHUB'
      and m.provider_symbol = 'GOOGL'
  );
