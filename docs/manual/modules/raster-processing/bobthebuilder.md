# BobTheBuilder

Builds an artifact into an elevation model: a dam, an embankment, a levee, a ramp. The artifact is given as a polygon, its outline, and as points with its elevations, as the crest of a dam. Inside the polygon, the module interpolates the elevations of the points and puts them into the raster; outside, the raster is not changed.

By default the artifact is only added: where the terrain is higher than the artifact, the terrain is kept. So the polygon of a dam can be drawn wider than the valley, into its slopes, and the dam ends where it meets them, without cutting them. With erosion enabled, the interpolated elevations replace the terrain everywhere in the polygon, also lowering it, as for an excavation or a channel.

The modified elevation model is the input of the hydrological analyses of the scenario with the artifact: the flow directions and the basins with a dam, the areas protected by a levee.

## Example

The example builds a dam across a valley of the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`. The polygon of the dam, `dam.geojson`, is 30 m thick and crosses the valley into its slopes; the points of its crest, `dam_crest.geojson`, are all at 1310 m:

```{literalinclude} ../examples/hm/bobthebuilder.groovy
:language: groovy
```

The two vectors, as they would be drawn in a GIS, here written as GeoJSON in the coordinates of the elevation model, declared by their `crs` member (EPSG:3003). The polygon of the dam, whose field `height` is used by the example of [RasterExtruder](rasterextruder.md):

```{literalinclude} ../maps/data/dam.geojson
:language: json
```

The points of the crest, with their elevation in the field `elev`:

```{literalinclude} ../maps/data/dam_crest.geojson
:language: json
```

:::{figure} ../../images/modules/BobTheBuilder_dam_3d.png
:alt: The elevation model with the dam in 3D, in the NWW Viewer, looking up the valley: the dam closes the valley as a wall, and ends where the slopes are higher than its crest.

The elevation model with the dam in 3D, in the [NWW Viewer](../../apps/nwwviewer.md), looking up the valley: the dam closes the valley as a wall, and ends where the slopes are higher than its crest. The output `dtm_dam.tif` is used as the terrain and draped with its elevation colors, under a half transparent map of its aspect, from [Aspect](../geomorphology/aspect.md), which brings out the faces of the dam.
:::

:::{figure} ../../images/modules/BobTheBuilder_dtm_dam-dtm_flanginec.png
:alt: The height of the dam over the terrain: highest over the bottom of the valley, zero on the slopes higher than the crest.

The height of the dam over the terrain: highest over the bottom of the valley, zero on the slopes higher than the crest.
:::

## Reference

```{include} ../generated/BobTheBuilder.md
```
