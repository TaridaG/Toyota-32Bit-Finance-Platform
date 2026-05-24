-- TEFAS TP2 (ör. Metro Portföy / Meteor Portföy serbest fon — kullanıcı talebi; TI2’den ayrı kod).
insert into instruments (symbol, name, type, exchange, active)
select 'FUND_TP2', 'TEFAS TP2 — Metro Portföy Serbest Fon', 'FUND', 'TEFAS', true
where not exists (select 1 from instruments where symbol = 'FUND_TP2');
