-- AVPGY + SARKY for Yahoo live + history (see market.tracked-stocks).
insert into instruments (symbol, name, type, exchange, active)
select v.symbol, v.name, 'STOCK', 'BIST', true
from (
    values
        ('AVPGY', 'Avrupakent Gayrimenkul Yatirim Ortakligi'),
        ('SARKY', 'Sarkuysan')
) as v(symbol, name)
where not exists (select 1 from instruments i where i.symbol = v.symbol);
