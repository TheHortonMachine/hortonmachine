# IntensityClassifierDebrisFlowTN

IntensityClassifierDebrisFlowTN classifies the intensity of a debris flow in three classes, 1 low, 2 medium and 3 high, following the guidelines of the Autonomous Province of Trento (TN). Four maps describe the event: the depth of the flow, its velocity outside of the river bed, the thickness of its deposits and the depth of the erosion. Each has a lower and an upper threshold: the intensity is high where any of them is over its upper threshold, medium where any is over its lower one, low elsewhere.

The default thresholds are 0.5 and 1 m for the depth and the deposits, 0.5 and 1 m/s for the velocity, 0.5 and 2 m for the erosion. The intensity maps of the events of 30, 100 and 200 years of return period are the input of [HazardClassifier](hazardclassifier.md).

## Example

The example makes up the four maps of a debris flow on a fan of 500 m at the end of the main channel of the sample area, as a model of debris flows would give them: deep and fast at the apex, stopping at the edge, with the deposits thickest in the middle and the erosion only near the apex.

```{literalinclude} ../examples/hm/intensityclassifierdebrisflowtn.groovy
:language: groovy
```

:::{figure} ../../images/modules/IntensityClassifierDebrisFlowTN_debris_intensity.png
:alt: The intensity of the made up debris flow: high near the apex, where the flow is deep and fast, medium in the middle of the fan, low at its edge.

The intensity of the made up debris flow: high near the apex, where the flow is deep and fast, medium in the middle of the fan, low at its edge.
:::

## Reference

```{include} ../generated/IntensityClassifierDebrisFlowTN.md
```
