-- Extra tracked crypto pairs (see market-data-service TrackedCryptoSymbols.SYMBOLS).

insert into instruments (symbol, name, type, exchange, active)
select 'DOGEUSDT', 'Dogecoin', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'DOGEUSDT');

insert into instruments (symbol, name, type, exchange, active)
select 'ADAUSDT', 'Cardano', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'ADAUSDT');

insert into instruments (symbol, name, type, exchange, active)
select 'AVAXUSDT', 'Avalanche', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'AVAXUSDT');

insert into instruments (symbol, name, type, exchange, active)
select 'LINKUSDT', 'Chainlink', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'LINKUSDT');

insert into instruments (symbol, name, type, exchange, active)
select 'TRXUSDT', 'TRON', 'CRYPTO', 'BINANCE', true
where not exists (select 1 from instruments where symbol = 'TRXUSDT');
