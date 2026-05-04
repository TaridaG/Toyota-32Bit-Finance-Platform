insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'YAHOO', 'GARAN.IS', i.id, 0, true
from instruments i
where i.symbol = 'GARAN'
  and i.type = 'STOCK'
  and not exists (
    select 1
    from mds_provider_instrument_mapping m
    where m.provider = 'YAHOO'
      and m.provider_symbol = 'GARAN.IS'
  );

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'YAHOO', 'THYAO.IS', i.id, 0, true
from instruments i
where i.symbol = 'THYAO'
  and i.type = 'STOCK'
  and not exists (
    select 1
    from mds_provider_instrument_mapping m
    where m.provider = 'YAHOO'
      and m.provider_symbol = 'THYAO.IS'
  );
