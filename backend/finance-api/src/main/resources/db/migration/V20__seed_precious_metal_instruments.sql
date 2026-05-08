insert into instruments (symbol, name, type, exchange, active)
select 'XAUTRY', 'Gold / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'XAUTRY');

insert into instruments (symbol, name, type, exchange, active)
select 'XAGTRY', 'Silver / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'XAGTRY');

insert into instruments (symbol, name, type, exchange, active)
select 'XPTTRY', 'Platinum / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'XPTTRY');

insert into instruments (symbol, name, type, exchange, active)
select 'XPDTRY', 'Palladium / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'XPDTRY');

insert into instruments (symbol, name, type, exchange, active)
select 'XCUTRY', 'Copper / TRY', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'XCUTRY');
