# LasHistogram

LasHistogram counts the points of a LAS file by their intensity, elevation or classification, in bins of equal width between the minimum and the maximum. The histogram is printed, shown in a chart window and written to a csv file. It is the first look at a survey: the intensity to choose the thresholds of [LasThresholder](lasthresholder.md), the elevation to see the layers of the vegetation.

## Example

The histogram of the intensity of the sample survey, in 30 bins, written to a table:

```{literalinclude} ../examples/hm/lashistogram.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasHistogram_las_intensity.png
:alt: The histogram of the intensity of the sample survey: most points between 0 and 220, with a peak around 170, and very few brighter points up to 420.

The histogram of the intensity of the sample survey: most points between 0 and 220, with a peak around 170, and very few brighter points up to 420.
:::

## Reference

```{include} ../generated/LasHistogram.md
```
