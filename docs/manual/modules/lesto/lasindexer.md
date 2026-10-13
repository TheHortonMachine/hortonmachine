# LasIndexer

LasIndexer indexes a folder of LAS files, so that it can be used as a single survey: the lidar modules take its main index, `index.lasfolder`, in place of a LAS file, and read only the parts of the files in the area they work on. Each file is rewritten with its points sorted by cells, next to the index of its cells.

## Example

The index of the four pieces of the sample survey written by [LasSplitter](lassplitter.md), in cells of 5 m, with the overview of their points:

```{literalinclude} ../examples/hm/lasindexer.groovy
:language: groovy
```

The folder `las_pieces` gets the main index `index.lasfolder` with its prj file, the overview `index.shp`, and for each piece the indexed file and its index, as `uni_bz_plot777_1_indexed.las` and `uni_bz_plot777_1_indexed.lasfix`. The modules then read the whole survey from `las_pieces/index.lasfolder`.

## Reference

```{include} ../generated/LasIndexer.md
```
