-- Remove Yahoo metal futures catalog rows (GC=F, SI=F, …) from MDS instrument catalog.
DELETE FROM mds_provider_instrument_mapping
WHERE provider = 'YAHOO'
  AND provider_symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F');

DELETE FROM mds_instrument_catalog
WHERE canonical_symbol IN ('GC=F', 'SI=F', 'HG=F', 'PA=F', 'PL=F');
