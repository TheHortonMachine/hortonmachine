package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public enum ProjectNeededParameterCodes implements IParametersCode {
    ACCURACY(0, "Accuracy to use to calculate a solution", " ", null, null, null), //
    A(1, "Coefficient of the pluviometric curve", " ", null, null, null), //
    N(2, "Exponent of the pluviometric curve", " ", null, null, null), //
    TAU(3, "Tangential bottom stress", " ", null, null, null), //
    G(4, "Fill degree", " ", null, null, null), //
    ALIGN(5, "Align mode", " ", null, null, null);

    private int code;
    private String key;
    private String description;
    private final String defaultValue;
    private final Double minRange;
    private final Double maxRange;
    
    private final static String  PROJECT_NEEDED_PAGE_NAME = "project needed project parameters";//$NON-NLS-1$
    private final static String  PROJECT_NEEDED_PAGE_TITLE = "needed parameters in project mode";//$NON-NLS-1$
    private final static String  PROJECT_NEEDED_PAGE_DESCRIPTION = "This field have to be setted  to project";//$NON-NLS-1$
    
    ProjectNeededParameterCodes( int code, String key, String description, String defaultValue, Double minRange, Double maxRange ) {
        this.code = code;
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public static ProjectNeededParameterCodes forCode( int i ) {
        ProjectNeededParameterCodes[] values = values();
        for( ProjectNeededParameterCodes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static ProjectNeededParameterCodes forKey( String key ) {
        ProjectNeededParameterCodes[] values = values();
        for( ProjectNeededParameterCodes type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }

    @Override
    public Double getMinRange() {
        // TODO Auto-generated method stub
        return minRange;
    }

    @Override
    public Double getMaxRange() {
        // TODO Auto-generated method stub
        return maxRange;
    }

    @Override
    public String getPageName() {
        // TODO Auto-generated method stub
        return PROJECT_NEEDED_PAGE_NAME;
    }

    @Override
    public String getPageTitle() {
        // TODO Auto-generated method stub
        return PROJECT_NEEDED_PAGE_TITLE;
    }

    @Override
    public String getPageDescription() {
        // TODO Auto-generated method stub
        return PROJECT_NEEDED_PAGE_DESCRIPTION;
    }
}
