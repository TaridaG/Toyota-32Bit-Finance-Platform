insert into instruments (symbol, name, type, exchange, active)
select 'TLDEP_MT01', 'TL Mevduat - 1 aya kadar', 'DEPOSIT', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TLDEP_MT01');

insert into instruments (symbol, name, type, exchange, active)
select 'TLDEP_MT02', 'TL Mevduat - 3 aya kadar', 'DEPOSIT', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TLDEP_MT02');

insert into instruments (symbol, name, type, exchange, active)
select 'TLDEP_MT03', 'TL Mevduat - 6 aya kadar', 'DEPOSIT', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TLDEP_MT03');

insert into instruments (symbol, name, type, exchange, active)
select 'TLDEP_MT04', 'TL Mevduat - 1 yila kadar', 'DEPOSIT', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TLDEP_MT04');

insert into instruments (symbol, name, type, exchange, active)
select 'TLDEP_MT05', 'TL Mevduat - 1 yil ve uzeri', 'DEPOSIT', 'TCMB', true
where not exists (select 1 from instruments where symbol = 'TLDEP_MT05');
