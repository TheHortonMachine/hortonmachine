# ErmRadiation

Computes the net radiation at the centroid of each sub-basin, as the balance of:

- the **shortwave** radiation from the sun: the position of the sun, the slope and aspect of the terrain, the shadows cast by the surrounding terrain and the sky view factor;
- the **longwave** radiation, from a clear sky model driven by the interpolated air temperature.

With daily data, `pDailySubSamples` sets how many positions of the sun are averaged over the day: 24 is the most accurate, 1 the fastest. `downscaleFactor` computes the radiation on a coarser DTM, much faster on large basins.

```{literalinclude} ../../examples/erm/04_radiation.groovy
:language: groovy
```

```{include} ../../generated/ErmRadiation.md
```
