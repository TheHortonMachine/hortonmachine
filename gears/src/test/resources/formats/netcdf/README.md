# NetCDF test data

## daymet_v4r1_tmax_2020_boulder.nc4, daymet_v4r1_srad_2020_boulder.nc4

Subsets of Daymet V4R1 daily data: 20 x 20 cells of 1 km around Boulder (Colorado, USA), 10 days from
2020-07-01 to 2020-07-10. NetCDF-4 files as served by the NASA Earthdata Hyrax OPeNDAP server, with the
Lambert conformal conic grid mapping of Daymet given only as CF parameters (no `crs_wkt`), the 2D `lat`/`lon`
auxiliary coordinates and the y axis from north to south.

- tmax: daily maximum temperature, degrees C
- srad: daily mean shortwave radiation, W/m2

Native grid: x from -437250 to -418250 m, y from -241000 to -260000 m (cell centers), 1000 m cells.

### Source and citation

Daymet data are open, the ORNL DAAC asks to cite them:

Thornton, M.M., R. Shrestha, Y. Wei, P.E. Thornton, S-C. Kao, and B.E. Wilson. 2022. Daymet: Daily Surface
Weather Data on a 1-km Grid for North America, Version 4 R1. ORNL DAAC, Oak Ridge, Tennessee, USA.
https://doi.org/10.3334/ORNLDAAC/2129

### How they were made

With Earthdata credentials in `~/.netrc` (`machine urs.earthdata.nasa.gov login ... password ...`), for `VAR` tmax
and srad:

```
curl -g -n -L -c cookies -b cookies -o daymet_v4r1_VAR_2020_boulder.nc4 \
  "https://opendap.earthdata.nasa.gov/collections/C2532426483-ORNL_CLOUD/granules/Daymet_Daily_V4R1.daymet_v4_daily_na_VAR_2020.nc.dap.nc4?dap4.ce=ENCODED"
```

where `ENCODED` is the URL encoded (`[`, `]`, `;`, `:`, `/`) constraint:

```
/VAR[182:191][5225:5244][4123:4142];/x[4123:4142];/y[5225:5244];/time[182:191];/lat[5225:5244][4123:4142];/lon[5225:5244][4123:4142];/lambert_conformal_conic
```
