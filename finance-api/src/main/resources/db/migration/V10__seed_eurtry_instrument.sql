insert into instruments (symbol, name, type, exchange, active)
select 'EURTRY', 'Euro', 'FX', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'EURTRY');
