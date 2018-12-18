package org.hortonmachine.style;

import java.util.List;

import org.geotools.styling.Rule;
import org.hortonmachine.gears.utils.style.FeatureTypeStyleWrapper;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.hortonmachine.gears.utils.style.StyleWrapper;

public final class WrapperUtilities {

    /**
     * Checks if the list of {@link Rule}s supplied contains one with the supplied name.
     * 
     * <p>If the rule is contained it adds an index to the name.
     * 
     * @param rulesWrapper the list of rules to check.
     * @param ruleName the name of the rule to find.
     * @return the new name of the rule.
     */
    public static String checkSameNameRule( List<RuleWrapper> rulesWrapper, String ruleName ) {
        int index = 1;
        String name = ruleName.trim();
        for( int i = 0; i < rulesWrapper.size(); i++ ) {
            RuleWrapper ruleWrapper = rulesWrapper.get(i);
            String tmpName = ruleWrapper.getName();
            if (tmpName == null) {
                continue;
            }

            tmpName = tmpName.trim();
            if (tmpName.equals(name)) {
                // name exists, change the name of the entering
                if (name.endsWith(")")) {
                    name = name.trim().replaceFirst("\\([0-9]+\\)$", "(" + (index++) + ")");
                } else {
                    name = name + " (" + (index++) + ")";
                }
                // start again
                i = 0;
            }
            if (index == 1000) {
                // something odd is going on
                throw new RuntimeException();
            }
        }
        return name;
    }

    /**
     * Checks if the list of {@link FeatureTypeStyleWrapper}s supplied contains one with the supplied name.
     * 
     * <p>If the rule is contained it adds an index to the name.
     * 
     * @param ftsWrapperList the list of featureTypeStyles to check.
     * @param ftsName the name of the featureTypeStyle to find.
     * @return the new name of the featureTypeStyle.
     */
    public static String checkSameNameFeatureTypeStyle( List<FeatureTypeStyleWrapper> ftsWrapperList, String ftsName ) {
        int index = 1;
        String name = ftsName.trim();
        for( int i = 0; i < ftsWrapperList.size(); i++ ) {
            FeatureTypeStyleWrapper ftsWrapper = ftsWrapperList.get(i);
            String tmpName = ftsWrapper.getName();
            if (tmpName == null) {
                continue;
            }

            tmpName = tmpName.trim();
            if (tmpName.equals(name)) {
                // name exists, change the name of the entering
                if (name.endsWith(")")) {
                    name = name.trim().replaceFirst("\\([0-9]+\\)$", "(" + (index++) + ")");
                } else {
                    name = name + " (" + (index++) + ")";
                }
                // start again
                i = 0;
            }
            if (index == 1000) {
                // something odd is going on
                throw new RuntimeException();
            }
        }
        return name;
    }

    /**
     * Checks if the list of {@link StyleWrapper}s supplied contains one with the supplied name.
     * 
     * <p>If the style is contained it adds an index to the name.
     * 
     * @param styles the list of style wrappers to check.
     * @param styleName the name of the style to find.
     * @return the new name of the style.
     */
    public static String checkSameNameStyle( List<StyleWrapper> styles, String styleName ) {
        int index = 1;
        String name = styleName.trim();
        for( int i = 0; i < styles.size(); i++ ) {
            StyleWrapper styleWrapper = styles.get(i);
            String tmpName = styleWrapper.getName();
            if (tmpName == null) {
                continue;
            }

            tmpName = tmpName.trim();
            if (tmpName.equals(name)) {
                // name exists, change the name of the entering
                if (name.endsWith(")")) {
                    name = name.trim().replaceFirst("\\([0-9]+\\)$", "(" + (index++) + ")");
                } else {
                    name = name + " (" + (index++) + ")";
                }
                // start again
                i = 0;
            }
            if (index == 1000) {
                // something odd is going on
                throw new RuntimeException();
            }
        }
        return name;
    }

}
