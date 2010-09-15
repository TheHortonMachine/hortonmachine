package org.jgrasstools.hortonmachine.externals.epanet.core;

@SuppressWarnings("nls")
public enum NodeParameters {
    EN_ELEVATION(0, "Elevation "), //
    EN_BASEDEMAND(1, "Base demand "), //
    EN_PATTERN(2, "Demand pattern index "), //
    EN_EMITTER(3, "Emitter coeff.  "), //
    EN_INITQUAL(4, "Initial quality "), //
    EN_SOURCEQUAL(5, "Source quality "), //
    EN_SOURCEPAT(6, "Source pattern index "), //
    EN_SOURCETYPE(7, "Source type (See note below) "), //
    EN_TANKLEVEL(8, "Initial water level in tank "), //
    EN_DEMAND(9, "Actual demand "), //
    EN_HEAD(10, "Hydraulic head "), //
    EN_PRESSURE(11, "Pressure "), //
    EN_QUALITY(12, "Actual quality "), //
    EN_SOURCEMASS(13, "Mass flow rate per minute of a chemical source "), //
    EN_INITVOLUME_STORAGETANK(14, "Initial water volume "), //
    EN_MIXMODEL_STORAGETANK(15, "Mixing model code (see below) "), //
    EN_MIXZONEVOL_STORAGETANK(16, "Inlet/Outlet zone volume in a 2-compartment tank "), //
    EN_TANKDIAM_STORAGETANK(17, "Tank diameter "), //
    EN_MINVOLUME_STORAGETANK(18, "Minimum water volume "), //
    EN_VOLCURVE_STORAGETANK(19, "Index of volume versus depth curve (0 if none assigned) "), //
    EN_MINLEVEL_STORAGETANK(20, "Minimum water level "), //
    EN_MAXLEVEL_STORAGETANK(21, "Maximum water level "), //
    EN_MIXFRACTION_STORAGETANK(22, "Fraction of total volume occupied by the inlet/outlet zone in a 2-compartment tank "), //
    EN_TANK_KBULK_STORAGETANK(23, "Bulk reaction rate coefficient ");

    private int code;
    private String description;
    NodeParameters( int code, String description ) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NodeParameters forCode( int i ) {
        NodeParameters[] values = values();
        for( NodeParameters type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
