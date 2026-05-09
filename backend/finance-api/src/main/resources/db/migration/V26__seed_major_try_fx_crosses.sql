insert into instruments (symbol, name, type, exchange, active)
select 'GBPTRY', 'British Pound / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'GBPTRY');

insert into instruments (symbol, name, type, exchange, active)
select 'JPYTRY', 'Japanese Yen / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'JPYTRY');

insert into instruments (symbol, name, type, exchange, active)
select 'AEDTRY', 'UAE Dirham / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'AEDTRY');
