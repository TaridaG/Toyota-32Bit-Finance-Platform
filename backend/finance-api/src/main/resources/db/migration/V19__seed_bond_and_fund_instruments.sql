insert into instruments (symbol, name, type, exchange, active)
select 'TRBOND2Y', 'TR Government Bond 2Y Yield', 'BOND', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TRBOND2Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRBOND5Y', 'TR Government Bond 5Y Yield', 'BOND', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TRBOND5Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRBOND10Y', 'TR Government Bond 10Y Yield', 'BOND', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TRBOND10Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRBOND1Y', 'TR Government Bond 1Y Yield', 'BOND', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TRBOND1Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRBOND3Y', 'TR Government Bond 3Y Yield', 'BOND', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TRBOND3Y');

insert into instruments (symbol, name, type, exchange, active)
select 'VOO', 'Vanguard S&P 500 ETF', 'FUND', 'FINNHUB', true
where not exists (select 1 from instruments where symbol = 'VOO');

insert into instruments (symbol, name, type, exchange, active)
select 'VTI', 'Vanguard Total Stock Market ETF', 'FUND', 'FINNHUB', true
where not exists (select 1 from instruments where symbol = 'VTI');

insert into instruments (symbol, name, type, exchange, active)
select 'QQQ', 'Invesco QQQ Trust', 'FUND', 'FINNHUB', true
where not exists (select 1 from instruments where symbol = 'QQQ');

insert into instruments (symbol, name, type, exchange, active)
select 'IVV', 'iShares Core S&P 500 ETF', 'FUND', 'FINNHUB', true
where not exists (select 1 from instruments where symbol = 'IVV');

insert into instruments (symbol, name, type, exchange, active)
select 'SPY', 'SPDR S&P 500 ETF Trust', 'FUND', 'FINNHUB', true
where not exists (select 1 from instruments where symbol = 'SPY');
