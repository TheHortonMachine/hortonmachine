# VectorCleaner

Fixes the invalid geometries of a vector, as polygons that cross themselves, and removes the features without a geometry. For polygons, as parcels digitized by hand, it also fills the small holes, cuts the narrow spikes, fills the narrow notches and gaps of the outline and finds or resolves the overlaps between neighbors. A second vector of lines shows where it changed something, to check the cleaning.

## Example

The example writes some parcels with the usual errors of digitizing, then cleans them of the holes up to 50 m², of what is narrower than 5 m and of the overlaps, which the larger parcel keeps:

```{literalinclude} ../examples/hm/vectorcleaner.groovy
:language: groovy
```

The parcels before the cleaning, each in its color. The bow-tie of parcel 1 crosses itself, parcel 2 has a small, a narrow and a big hole, parcel 3 a spike and parcel 4 a notch, parcels 5 and 6 overlap along their border and the two parts of parcel 7 are divided by a narrow gap:

```{image} ../../images/modules/VectorCleaner_parcels_userdata.png
:alt: The parcels before the cleaning
```

After the cleaning, with the changes outlined in blue, the bow-tie is made of two valid triangles, the small and the narrow holes are filled while the big one stays, the spike is cut, the notch and the gap are filled and the overlap belongs to parcel 6 only:

```{image} ../../images/modules/VectorCleaner_parcels_cleaned_userdata.png
:alt: The parcels after the cleaning
```

The module prints the counts and the areas of what it changed, as the total area before and after the cleaning.

## Reference

```{include} ../generated/VectorCleaner.md
```
