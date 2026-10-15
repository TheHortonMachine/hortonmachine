# TmsWms2Geotiff

Downloads the map tiles of an online service over a region and joins them into a single GeoTIFF image, in Web Mercator. It brings a background map, as an orthophoto or a topographic map, into the analyses that need a raster, or offline. The service can be a tile service, with the tile indexes in its url, as most of the web maps, or a WMS.

The size of the image grows fast with the zoom level: each level halves the size of the cells and quadruples the tiles. The terms of use of most of the services limit the bulk downloads of tiles, so the module is meant for small regions.

## Details

```{include} ../generated/TmsWms2Geotiff_details.md
```

## Parameters

```{include} ../generated/TmsWms2Geotiff.md
```

## Example

The example downloads the tiles over the basin of [ExtractBasin](../dem-manipulation/extractbasin.md); the url is a placeholder for the one of the service to use:

```{literalinclude} ../examples/hm/tmswms2geotiff.groovy
:language: groovy
```
