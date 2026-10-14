# Gura sample data

The sample data of the InVEST Seasonal Water Yield model (InVEST 3.20.2, `Seasonal_Water_Yield.zip`), used by the example of the Seasonal Water Yield modules of the manual. Gura is a small sub-watershed in Kenya, where the Natural Capital Project worked with partners to inform the creation of a water fund for the city of Nairobi.

The data are provided as an example only, to learn the model. They are not authoritative and must not be used as they are in real projects, in particular the coefficients of the biophysical table, which are globally averaged values from the literature.

Sources, as given by the Natural Capital Project:

- `DEM_gura.tif`: NASA Shuttle Radar Topography Mission (SRTM) 30 m elevation, resampled to the 15 m of the land use map;
- `land_use_gura.tif`: The Nature Conservancy (TNC), an update of the Africover land use maps from satellite imagery, stakeholder maps and ground truth points;
- `soil_group_gura.tif`: FutureWater, Global Maps of Soil Hydraulic Properties, version 1.2, 2016;
- `precipitation/`: CHELSA monthly precipitation, version 1.2 (Karger, D. N., Conrad, O., Böhner, J., Kawohl, T., Kreft, H., Soria-Auza, R. W., Zimmermann, N. E., Linder, H. P., Kessler, M. (2017). Climatologies at high resolution for the earth's land surface areas. Scientific Data, 4, 170122);
- `et0/`: derived from the CHELSA monthly precipitation and temperatures with the modified Hargreaves method;
- `rain_events_gura.csv`: IWMI Online Climate Summary Service Portal, International Water Management Institute;
- `biophysical_table_gura_SWY.csv`: globally averaged values from a variety of literature sources.

The land use and the elevation were stored with lossless compression; their values are those of the original files.
