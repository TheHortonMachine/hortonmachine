# ImageMosaicCreator

Creates the index of a GeoTools image mosaic in a folder of georeferenced images, as the tiles of an orthophoto or of an elevation model. The folder can then be read as one raster, by GeoServer and by the modules that read image mosaics, without merging the tiles in a single file.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/imagemosaiccreator.groovy
:language: groovy
```

:::{figure} ../../images/modules/ImageMosaicCreator_dtm_tiles.png
:alt: The footprints of the two elevation models in the index of the mosaic.

The footprints of the two elevation models in the index of the mosaic, `dtm_tiles.shp`.
:::

## Reference

```{include} ../generated/ImageMosaicCreator.md
```
