# Mapcalc

Mapcalc computes a new raster from other rasters with a script in the Jiffle language: the value of each output cell is computed from the values of the input maps in the same cell, or in the cells around it, with arithmetic, conditions, loops and functions. It is the module behind the [Map Calculator](../../apps/mapcalc.md) application, whose chapter describes the language with examples.

Unlike the application, the module needs the full Jiffle script, with the images block that declares the maps: the inputs with `read`, the output with `write`. The maps are known by their file name without extension. The module is hidden in the Spatial Toolbox, where the Map Calculator replaces it, and is meant for scripts.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/mapcalc.groovy
:language: groovy
```

:::{figure} ../../images/modules/Mapcalc_filled.png
:alt: The depth of the depressions of the sample area filled by Pitfiller, in meters: the depitted elevation minus the elevation, where it is above 0.

The depth of the depressions of the sample area filled by Pitfiller, in meters: the depitted elevation minus the elevation, where it is above 0.
:::

## Reference

```{include} ../generated/Mapcalc.md
```
