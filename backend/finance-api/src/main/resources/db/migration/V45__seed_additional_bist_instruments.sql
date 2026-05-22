-- Additional BIST equities for Yahoo live + history backfill (see market.tracked-stocks).
insert into instruments (symbol, name, type, exchange, active)
select v.symbol, v.name, 'STOCK', 'BIST', true
from (
    values
        ('AKBNK', 'Akbank'),
        ('AVGYO', 'Avrasya Gayrimenkul Yatirim Ortakligi'),
        ('EREGL', 'Eregli Demir ve Celik'),
        ('BIMAS', 'BIM Birlesik Magazalar'),
        ('TUPRS', 'Tupras'),
        ('ISCTR', 'Turkiye Is Bankasi'),
        ('SAHOL', 'Haci Omer Sabanci Holding')
) as v(symbol, name)
where not exists (select 1 from instruments i where i.symbol = v.symbol);
