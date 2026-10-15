# Nabla

Calculates the nabla, or laplacian, of the elevation: the sum of its second derivatives along two perpendicular directions. It is the simplest measure of the curvature of the terrain, without a direction: positive in the concave sites, as the valleys and the hollows, negative in the convex ones, as the ridges and the noses. Unlike the [Curvatures](curvatures.md), it does not separate the curvature along the slope from the one across it.

As every second derivative, the nabla amplifies the noise of the elevation: on detailed elevation models, a coarser resolution gives more readable maps.

With a threshold, the module gives a mask of the clearly convex sites instead, with 1 where the nabla is lower than minus the threshold.

## Details

```{include} ../generated/Nabla_details.md
```

## Parameters

```{include} ../generated/Nabla.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/nabla.groovy
:language: groovy
```

:::{figure} ../../images/modules/Nabla_nabla.png
:alt: The nabla of the sample area: positive, in red, in the channels and the hollows, negative on the ridges. The colors span from the 2nd to the 98th percentile of the values.

The nabla of the sample area: positive, in red, in the channels and the hollows, negative on the ridges. The colors span from the 2nd to the 98th percentile of the values.
:::
