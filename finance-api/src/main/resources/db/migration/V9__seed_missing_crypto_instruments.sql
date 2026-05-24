insert into instruments (symbol, name, type, exchange, active)
select 'BNBUSDT', 'BNB', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'BNBUSDT');

insert into instruments (symbol, name, type, exchange, active)
select 'SOLUSDT', 'Solana', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'SOLUSDT');

insert into instruments (symbol, name, type, exchange, active)
select 'XRPUSDT', 'XRP', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'XRPUSDT');
