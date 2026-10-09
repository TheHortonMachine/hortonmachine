# HazardClassifier

HazardClassifier combines the intensity maps of the events of 30, 100 and 200 years of return period, from [IntensityClassifierFlood](intensityclassifierflood.md) or [IntensityClassifierDebrisFlowTN](intensityclassifierdebrisflowtn.md), into two hazard maps, IP1 and IP2, with the classes 2 low, 3 medium and 4 high. Each intensity of each event gets a score, higher for the more intense and the more frequent events, and the highest score of the cell gives its hazard; the two maps differ in where the classes change. The tables are in the notes of the reference below.

A cell without a value in an intensity map is not reached by that event.

## Example

The example combines the three floods of the example of [IntensityClassifierFlood](intensityclassifierflood.md):

```{literalinclude} ../examples/hm/hazardclassifier.groovy
:language: groovy
```

:::{figure} ../../images/modules/HazardClassifier_hazard_ip1.png
:alt: The hazard IP1: high in the channel, medium where the floods of 30 or 100 years reach the floodplain, low where only the one of 200 years does.

The hazard IP1: high in the channel, medium where the floods of 30 or 100 years reach the floodplain, low where only the one of 200 years does.
:::

:::{figure} ../../images/modules/HazardClassifier_hazard_ip2.png
:alt: The hazard IP2 of the same floods: medium only where the flood of 30 years reaches the floodplain.

The hazard IP2 of the same floods: the low intensities of the floods of 100 and 200 years are a low hazard, so the floodplain is medium only where the flood of 30 years reaches it.
:::

## Reference

```{include} ../generated/HazardClassifier.md
```
