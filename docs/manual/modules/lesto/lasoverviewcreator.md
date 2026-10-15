# LasOverviewCreator

LasOverviewCreator draws the extent of each LAS file of a folder as a rectangle, with the name of the file, its number of points and its range of elevations. It reads only the headers of the files, so it is quick also on large surveys, to see how they are divided and where each file is.

## Details

```{include} ../generated/LasOverviewCreator_details.md
```

## Parameters

```{include} ../generated/LasOverviewCreator.md
```

## Example

The overview of the four pieces of the sample survey written by [LasSplitter](lassplitter.md):

```{literalinclude} ../examples/hm/lasoverviewcreator.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasOverviewCreator_las_pieces_overview_points.png
:alt: The four rectangles of the pieces, filled by their number of points: the north-east one, red, with 9,992 points, the denser part of the survey, the others, in yellow, with 7,745 to 7,883.

The four rectangles of the pieces, filled by their number of points: the north-east one, red, with 9,992 points, the denser part of the survey, the others, in yellow, with 7,745 to 7,883.
:::
