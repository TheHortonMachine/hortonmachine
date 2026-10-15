# VectorTransformer

Moves the geometries of a vector: rotates them, all together, around a center, and translates them. It puts in place a survey made in local coordinates, or a design to try in another position, without changing the crs.

## Details

```{include} ../generated/VectorTransformer_details.md
```

## Parameters

```{include} ../generated/VectorTransformer.md
```

## Example

The example turns the cells of [GridsGenerator](gridsgenerator.md) by 30 degrees clockwise around their lower left corner, and moves them 300 m east:

```{literalinclude} ../examples/hm/vectortransformer.groovy
:language: groovy
```

The moved grid in red, over the original one in blue: the corner of the rotation, at the lower left of the original grid, is now 300 m to its east:

```{image} ../../images/modules/VectorTransformer_grid_rotated.png
:alt: The grid rotated by 30 degrees and moved 300 m east, over the original grid
```
