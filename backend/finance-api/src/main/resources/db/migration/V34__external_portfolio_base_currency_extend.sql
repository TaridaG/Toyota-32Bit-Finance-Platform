-- Allow neutral / multi-currency portfolio accounts (e.g. MIXED) beyond ISO-4217 3-letter codes.
alter table external_portfolios
    alter column base_currency type varchar(16);
