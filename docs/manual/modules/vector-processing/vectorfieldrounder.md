# VectorFieldRounder

Rounds the values of a numeric field of a vector, as the statistics computed by other modules with more decimals than they mean, to make the tables lighter and easier to read.

## Details

```{include} ../generated/VectorFieldRounder_details.md
```

## Parameters

```{include} ../generated/VectorFieldRounder.md
```

## Example

The example rounds to the meter the mean elevation that [RasterCatToFeatureAttribute](rastercattofeatureattribute.md) computed for the cells of the grid:

```{literalinclude} ../examples/hm/vectorfieldrounder.groovy
:language: groovy
```

The first cells, before and after:

| id | elev_avg before | elev_avg after |
|---|---|---|
| 0 | 1078.607 | 1079 |
| 1 | 1303.779 | 1304 |
| 2 | 1520.539 | 1521 |
| 3 | 1742.999 | 1743 |
