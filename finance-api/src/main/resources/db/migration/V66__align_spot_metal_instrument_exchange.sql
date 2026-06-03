-- Spot metals use Minted/Yahoo-derived TRY/troy-oz pipeline, not TCMB gram archive backfill.

UPDATE instruments
SET exchange = 'COMPOSITE_FX'
WHERE symbol IN ('XAUTRY', 'XAGTRY', 'XPTTRY', 'XPDTRY', 'XCUTRY')
  AND exchange = 'TCMB';
