package org.hortonmachine.gears.io.stac;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum EarthAwsElement84Sentinel2Bands {
    aot("Aerosol optical thickness (AOT)"), //
    blue("Blue (band 2) - 10m"), //
    ca("Coastal aerosol (band 1) - 60m"), //
    green("Green (band 3) - 10m"), //
    nir1("NIR 1 (band 8) - 10m"), //
    nir2("NIR 2 (band 8A) - 20m"), //
    nir3("NIR 3 (band 9) - 60m"), //
    red("Red (band 4) - 10m"), //
    rededge1("Red edge 1 (band 5) - 20m"), //
    rededge2("Red edge 2 (band 6) - 20m"), //
    rededge3("Red edge 3 (band 7) - 20m"), //
    scl("Scene classification map (SCL)"), //
    swir1("SWIR 1 (band 11) - 20m"), //
    swir2("SWIR 2 (band 12) - 20m"), //
    wvp("Water vapour (WVP)");//

    private String realName;

    EarthAwsElement84Sentinel2Bands( String realName ) {
        this.realName = realName;
    }

    public String getRealName() {
        return realName;
    }

    public List<String> getShortNames() {
        return Arrays.asList(values()).stream().map(sb -> sb.name()).collect(Collectors.toList());
    }
}
