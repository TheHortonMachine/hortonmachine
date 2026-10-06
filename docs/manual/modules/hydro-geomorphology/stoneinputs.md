# StoneInputs

StoneInputs prepares the input maps of the [Stone](stone.md) rockfall model from as little as a DEM: the sources, the friction and the normal and tangential restitution coefficients. All the maps get the grid of the DEM.

## The sources

When no sources map is given, the sources are the cells steeper than a threshold (`pSourceSlope`, 45 degrees by default), each throwing the same number of boulders (`pBouldersPerSource`, 1 by default). The slope is calculated with the method of Horn, as `r.slope.aspect` of GRASS GIS, and can't be calculated for the outer cells of the DEM, which are never sources. A simple slope threshold is only a first guess of the source areas: better ones come from the knowledge of the area or from statistical methods.

An existing sources map can also be given: its values are kept. In both cases the valid cells of the optional stop areas map (`inStopAreas`), as lakes or protection works, become stop cells (-1), where the boulders stop.

## Friction and restitutions

The coefficients come from a map of lithological classes (`inLithology`), translated with a table of the coefficients of each class. Without a table, the built-in one has the 19 lithological classes used for Italy by Alvioli et al. (2021):

| Code | Lithological class | Friction | Normal restitution [%] | Tangential restitution [%] |
|---|---|---|---|---|
| 0 | Unclassified | 0.65 | 35 | 55 |
| 1 | Anthropic deposits | 0.65 | 35 | 55 |
| 2 | Alluvial, lacustrine, marine, eluvial and colluvial deposits | 0.80 | 15 | 40 |
| 3 | Coastal deposits | 0.65 | 35 | 55 |
| 4 | Landslides | 0.65 | 35 | 55 |
| 5 | Glacial deposits | 0.65 | 35 | 55 |
| 6 | Loosely packed clastic deposits | 0.35 | 45 | 55 |
| 7 | Consolidated clastic deposits | 0.40 | 55 | 65 |
| 8 | Marl | 0.40 | 55 | 65 |
| 9 | Carbonates-siliciclastic and marl sequence | 0.35 | 60 | 70 |
| 10 | Chaotic rocks and mélange | 0.35 | 45 | 55 |
| 11 | Flysch | 0.40 | 55 | 65 |
| 12 | Carbonate rocks | 0.30 | 65 | 75 |
| 13 | Evaporites | 0.35 | 45 | 55 |
| 14 | Pyroclastic rocks and ignimbrites | 0.40 | 55 | 65 |
| 15 | Lava and basalts | 0.30 | 65 | 75 |
| 16 | Intrusive igneous rocks | 0.30 | 65 | 75 |
| 17 | Schists | 0.35 | 60 | 70 |
| 18 | Non-schists | 0.30 | 65 | 75 |
| 19 | Lakes and glaciers | 0.95 | 10 | 10 |

Other classifications need their own table (`inLithologyTable`), a CSV file with a line for each class: the code, the friction, the normal and the tangential restitution in percent. Empty lines, lines starting with `#` and a header line are skipped:

```text
# coefficients of my geological map
code,friction,nrest,trest
1,0.80,15,40
2,0.30,65,75
```

Where the lithology map has no value, or a code that is not in the table, and everywhere if no lithology map is given, the coefficients of the default class of the built-in table are used (`pDefaultLithology`). Uniform coefficients from a single class are only a first screening: they ignore, for example, the soft deposits of the valleys that stop the boulders sooner.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/stoneinputs.groovy
:language: groovy
```

:::{figure} ../../images/modules/StoneInputs_sources.png
:alt: The sources of the sample area: the cells steeper than 45 degrees, each throwing 10 boulders.

The sources of the sample area: the cells steeper than 45 degrees, each throwing 10 boulders.
:::

## Reference

```{include} ../generated/StoneInputs.md
```
