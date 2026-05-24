insert into instruments (symbol, name, type, exchange, active)
select 'FUND_IVY', 'IVY Fund Placeholder', 'STOCK', 'BIST', true
where not exists (select 1 from instruments where symbol = 'FUND_IVY');
