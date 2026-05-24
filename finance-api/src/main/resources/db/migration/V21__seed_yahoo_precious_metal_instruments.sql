insert into instruments (symbol, name, type, exchange, active)
select 'GC=F', 'Gold Futures', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'GC=F');

insert into instruments (symbol, name, type, exchange, active)
select 'SI=F', 'Silver Futures', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'SI=F');

insert into instruments (symbol, name, type, exchange, active)
select 'HG=F', 'Copper Futures', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'HG=F');

insert into instruments (symbol, name, type, exchange, active)
select 'PA=F', 'Palladium Futures', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'PA=F');

insert into instruments (symbol, name, type, exchange, active)
select 'PL=F', 'Platinum Futures', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'PL=F');
