-- Magnificent-7 adjacent NASDAQ equities for Finnhub live + history (see providers.finnhub.symbols / market.tracked-stocks).
insert into instruments (symbol, name, type, exchange, active)
select v.symbol, v.name, 'STOCK', 'NASDAQ', true
from (
    values
        ('TSLA', 'Tesla Inc.'),
        ('META', 'Meta Platforms Inc.'),
        ('AVGO', 'Broadcom Inc.'),
        ('AMD', 'Advanced Micro Devices Inc.'),
        ('NFLX', 'Netflix Inc.'),
        ('INTC', 'Intel Corp.'),
        ('CSCO', 'Cisco Systems Inc.')
) as v(symbol, name)
where not exists (select 1 from instruments i where i.symbol = v.symbol);
