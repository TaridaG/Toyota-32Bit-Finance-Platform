insert into instruments (symbol, name, type, exchange, active)
select 'GARAN', 'Garanti BBVA', 'STOCK', 'BIST', true
where not exists (select 1 from instruments where symbol = 'GARAN');

insert into instruments (symbol, name, type, exchange, active)
select 'THYAO', 'Turkish Airlines', 'STOCK', 'BIST', true
where not exists (select 1 from instruments where symbol = 'THYAO');
