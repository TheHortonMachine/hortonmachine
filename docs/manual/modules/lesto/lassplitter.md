# LasSplitter

LasSplitter splits a LAS file into a grid of smaller files, of equal size, to process a large survey in pieces or to share a part of it. [LasMerger](lasmerger.md) merges them back.

## Example

The sample survey split in 2 x 2 pieces of about 30 x 30 m, in the folder `las_pieces`:

```{literalinclude} ../examples/hm/lassplitter.groovy
:language: groovy
```

The four pieces are `uni_bz_plot777_1.las`, the south-west one, to `uni_bz_plot777_4.las`, the north-east one, each with its prj file.

## Reference

```{include} ../generated/LasSplitter.md
```
