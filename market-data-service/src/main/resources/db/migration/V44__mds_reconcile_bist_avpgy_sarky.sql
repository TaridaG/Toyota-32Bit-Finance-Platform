-- Reconcile when finance-api V49 ran after V43 (instruments did not exist yet).
insert into mds_instrument_catalog (instrument_id, canonical_symbol, asset_class, base_currency, quote_currency, active)
select i.id, i.symbol, 'STOCK', null, 'TRY', true
from instruments i
where i.symbol in ('AVPGY', 'SARKY')
  and i.exchange = 'BIST'
  and not exists (select 1 from mds_instrument_catalog c where c.instrument_id = i.id);

insert into mds_provider_instrument_mapping (provider, provider_symbol, instrument_id, priority, active)
select 'YAHOO', i.symbol || '.IS', i.id, 0, true
from instruments i
where i.symbol in ('AVPGY', 'SARKY')
  and i.exchange = 'BIST'
  and not exists (
    select 1
    from mds_provider_instrument_mapping m
    where m.provider = 'YAHOO'
      and m.provider_symbol = i.symbol || '.IS'
  );
