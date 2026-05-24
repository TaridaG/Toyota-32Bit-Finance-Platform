insert into instruments (symbol, name, type, exchange, active)
select 'AAPL', 'Apple Inc.', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'AAPL');

insert into instruments (symbol, name, type, exchange, active)
select 'AMZN', 'Amazon.com Inc.', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'AMZN');

insert into instruments (symbol, name, type, exchange, active)
select 'NVDA', 'NVIDIA Corp.', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'NVDA');

insert into instruments (symbol, name, type, exchange, active)
select 'MSFT', 'Microsoft Corp.', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'MSFT');

insert into instruments (symbol, name, type, exchange, active)
select 'GOOGL', 'Alphabet Inc. Class A', 'STOCK', 'NASDAQ', true
where not exists (select 1 from instruments where symbol = 'GOOGL');
