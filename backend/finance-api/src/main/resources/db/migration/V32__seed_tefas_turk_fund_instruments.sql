-- TEFAS-tracked Turkish funds: para piyasası (TI2, AFT, AFA) + kıymetli maden / altın (AFO, GTA).
-- Canonical symbols follow existing FUND_* convention for market-data TEFAS mappings.
insert into instruments (symbol, name, type, exchange, active)
select 'FUND_TI2', 'TEFAS TI2 — para piyasası (örnek)', 'FUND', 'TEFAS', true
where not exists (select 1 from instruments where symbol = 'FUND_TI2');

insert into instruments (symbol, name, type, exchange, active)
select 'FUND_AFT', 'TEFAS AFT — para piyasası (örnek)', 'FUND', 'TEFAS', true
where not exists (select 1 from instruments where symbol = 'FUND_AFT');

insert into instruments (symbol, name, type, exchange, active)
select 'FUND_AFA', 'TEFAS AFA — para piyasası (örnek)', 'FUND', 'TEFAS', true
where not exists (select 1 from instruments where symbol = 'FUND_AFA');

insert into instruments (symbol, name, type, exchange, active)
select 'FUND_AFO', 'TEFAS AFO — kıymetli maden / altın (örnek)', 'FUND', 'TEFAS', true
where not exists (select 1 from instruments where symbol = 'FUND_AFO');

insert into instruments (symbol, name, type, exchange, active)
select 'FUND_GTA', 'TEFAS GTA — kıymetli maden / altın (örnek)', 'FUND', 'TEFAS', true
where not exists (select 1 from instruments where symbol = 'FUND_GTA');
