# PointCloudMaximaFinder

PointCloudMaximaFinder finds the tops of the trees directly in the points of the survey: a point is a top when no other point around it is higher, within a radius that grows with its height as the crowns do. It works at the full precision of the points, without a raster in between, but compares every point with all the others of its area: large surveys are processed in regions. [RasterMaximaFinder](rastermaximafinder.md) does the same on a canopy height model.

## Example

The tops of the sample survey higher than 5 m over the elevation model, with the dynamic radius up to 3 m:

```{literalinclude} ../examples/hm/pointcloudmaximafinder.groovy
:language: groovy
```

:::{figure} ../../images/modules/PointCloudMaximaFinder_tops_points.png
:alt: The 147 tops, red dots, over the canopy height model in green: a top in each crown, with a few more on the edges of the crowns and along the border of the survey.

The 147 tops, red dots, over the canopy height model in green: a top in each crown, with a few more on the edges of the crowns and along the border of the survey.
:::

## Reference

```{include} ../generated/PointCloudMaximaFinder.md
```
