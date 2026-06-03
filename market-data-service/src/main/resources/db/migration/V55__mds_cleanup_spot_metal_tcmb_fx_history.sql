-- Remove TCMB gram-scale spot metal FX history rows (incompatible with Minted/Yahoo TRY/troy-oz series).

DELETE FROM mds_fx_rate_history
WHERE canonical_symbol IN ('XAUTRY', 'XAGTRY', 'XPTTRY', 'XPDTRY', 'XCUTRY')
  AND provider IN ('TCMB', 'EVDS', 'TCMB_ARCHIVE');
