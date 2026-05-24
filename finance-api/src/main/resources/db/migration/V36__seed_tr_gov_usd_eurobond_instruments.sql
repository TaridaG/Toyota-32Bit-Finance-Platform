insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD1Y', 'Turkey USD Gov 1Y (GTUSDTR1Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD1Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD2Y', 'Turkey USD Gov 2Y (GTUSDTR2Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD2Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD3Y', 'Turkey USD Gov 3Y (GTUSDTR3Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD3Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD4Y', 'Turkey USD Gov 4Y (GTUSDTR4Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD4Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD5Y', 'Turkey USD Gov 5Y (GTUSDTR5Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD5Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD6Y', 'Turkey USD Gov 6Y (GTUSDTR6Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD6Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD8Y', 'Turkey USD Gov 8Y (GTUSDTR8Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD8Y');

insert into instruments (symbol, name, type, exchange, active)
select 'TRGOVUSD15Y', 'Turkey USD Gov 15Y (GTUSDTR15Y:GOV)', 'BOND', 'YAHOO', true
where not exists (select 1 from instruments where symbol = 'TRGOVUSD15Y');
