# AdaptiveTinFilter

AdaptiveTinFilter separates the ground from the vegetation and the buildings in a lidar survey, and interpolates it into an elevation model. It grows a triangulated surface of the ground from below: it starts from the lowest point of each square of a coarse grid, and at each iteration adds to each triangle the nearest point close and flat enough to it, as in the adaptive TIN filter of Axelsson (2000). The thresholds of distance and angle adapt to the terrain, computed again at each iteration on the points added.

## Details

```{include} ../generated/AdaptiveTinFilter_details.md
```

## Parameters

```{include} ../generated/AdaptiveTinFilter.md
```

## Example

The ground of the sample survey, on the grid of its elevation model, starting from squares of 10 m:

```{literalinclude} ../examples/hm/adaptivetinfilter.groovy
:language: groovy
```

:::{figure} ../../images/modules/AdaptiveTinFilter_dtm_tin.png
:alt: The elevation model of the ground extracted from the survey, from 1897 to 1927 m: the road at the north-west corner and the slope rising to the south-east. The strips along the south and east borders, outside of the triangles of the seeds, are empty.

The elevation model of the ground extracted from the survey, from 1897 to 1927 m: the road at the north-west corner and the slope rising to the south-east. The strips along the south and east borders, outside of the triangles of the seeds, are empty.
:::

:::{figure} ../../images/modules/AdaptiveTinFilter_dtm_tin-dtm_777.png
:alt: The difference from the elevation model of the survey, within a few decimeters almost everywhere: half of the cells differ by less than 10 cm.

The difference from the elevation model of the survey, within a few decimeters almost everywhere: half of the cells differ by less than 10 cm.
:::
