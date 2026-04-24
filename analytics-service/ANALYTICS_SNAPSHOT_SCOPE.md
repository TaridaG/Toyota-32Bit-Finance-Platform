# Analytics snapshot ingestion scope (Stage I)

## FX (`market.fx.snapshot.updated`)

- Consumed by `FxSnapshotUpdatedConsumer` using the same candle / MA / RSI / trend pipeline as `market.price.updated`.
- Price series uses mid, else bid, else ask; `instrumentId` preferred with finance catalog validation and legacy symbol fallback on `canonicalSymbol`.

## Fund (`market.fund.snapshot.updated`)

- Not consumed in analytics in Stage I. NAV series share the same numeric pipeline as trade prices but imply different semantics (official NAV cadence, stale NAV rules, corporate actions).
- Next step: decide between (a) dedicated fund NAV aggregates, (b) a separate read model, or (c) opt-in ingestion with a distinct `PriceType`/topic partition strategy aligned with finance-api.

## Stage J (finance-api semantics)

- Finance now persists FX snapshots as `PriceType.FX_MID` and fund NAV as `PriceType.FUND_NAV`; crypto / exchange ticks remain `MARKET` (or other types from the market event).
- Analytics FX consumer still feeds mid/bid/ask into the same candle pipeline as crypto prices; it does not yet distinguish `FX_MID` in storage. Fund NAV remains out of scope here until a dedicated analytics model is chosen.
