# Home recomposition measurement

## Probe and result

The foundation route read `bands.band` directly in its composition scope. Favorites bounds arrive
through `onGloballyPositioned`, and `HomeActionsFactory` updates `BandEditing.favoritesBand` while
the home moves. Each distinct band therefore invalidated the reading route scope.

Before changing that subscription, `BandRecompositionTest.baselineBandRead` exercised the actual
`BandEditing` model with 120 distinct updates and a composition counter around the original read
pattern: **121 compositions**, including the initial one.

`ObserveScrubBand` now contains the state read. The route passes the stable band owner into the
small consumers. The same focused probe records:

| Scope | Compositions, including initial |
|---|---:|
| Original outer read pattern | 121 |
| Outer scope with isolated read | 1 |
| Visible band consumer | 121 |
| Hidden consumer without a subscription | 1 |

Run with:

```bash
NIKE_PERF=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.BandRecompositionTest"
```

These are synthetic snapshot/recomposition counts. They reproduce the route's subscription pattern
and use the production isolation wrapper; they do not measure full-route duration, frame time,
input latency or GPU work. Home screenshot tests cover the resulting layout and edit controls.

## Changes supported by the result

- Remove the moving band read from `HomeRoute`.
- Read it inside edge zones/exclusions and idle/editor overlays.
- Keep the hidden app-list overlay composed, but unsubscribe it from band movement until reveal.
- Keep the actual band geometry and persisted hand-set range unchanged.

`NikeNavHost` still collects shared settings. Code review found preference-driven invalidation
there, but this session did not measure a per-frame settings emission. No speculative global-state
rewrite was made. Home scrolling and inertia were not changed.

## Device follow-up

A release-like `perf` build has R8/resource shrinking and the debug signature. It is distinct from
the debug package (`dev.nikelauncher.perf`). Compare scrolling, edge scrubbing and app-list opening
on the Pixel 9 with a frame trace or Macrobenchmark. No device frame trace was available here.
A generated Baseline Profile is still absent; it must be recorded and validated on a device.
