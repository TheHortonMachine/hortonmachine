# RasterToCog

Converts a raster into a Cloud Optimized GeoTIFF (COG): a GeoTIFF organized so that it can be read efficiently from a web server. The image is cut in square tiles, with overviews at lower resolutions, and the index of all of them is at the beginning of the file. So a client, as a web map, QGIS or GDAL, reads only the tiles of the area and of the resolution it shows, with a few HTTP range requests, without downloading the whole file.

The module writes the COG in pure Java, as the COG driver of GDAL does, with a lossless compression, Deflate or LZW. The overviews are computed from 2x2 cells, averaging the values of continuous data, as elevations, and taking the most frequent value of classes and codes: the default, *auto*, chooses from the data type. Large GeoTIFFs are read by windows, so they don't need to fit in memory.

A COG is still a GeoTIFF, readable by any GIS: converting the rasters to publish, as on a [STAC](../../apps/stacbrowser.md) catalog, makes them usable from the web.

## Details

```{include} ../generated/RasterToCog_details.md
```

## Parameters

```{include} ../generated/RasterToCog.md
```

## Example

The example converts the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, with the default settings:

```{literalinclude} ../examples/hm/rastertocog.groovy
:language: groovy
```
