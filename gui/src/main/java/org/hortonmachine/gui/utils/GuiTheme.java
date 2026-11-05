/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gui.utils;

import java.util.function.Supplier;

import javax.swing.LookAndFeel;

import org.hortonmachine.gui.themes.HMDarkLaf;
import org.hortonmachine.gui.themes.HMLightLaf;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDeepOceanIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialOceanicIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialPalenightIJTheme;

/**
 * The look and feel themes the HortonMachine gui can use.
 *
 * <p>The theme is stored in the preferences under its enum name.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public enum GuiTheme {
    HM_LIGHT(HMLightLaf.NAME, false, HMLightLaf::new), //
    HM_DARK(HMDarkLaf.NAME, true, HMDarkLaf::new), //
    MATERIAL_LIGHTER("Material Lighter", false, FlatMTMaterialLighterIJTheme::new), //
    MATERIAL_DARKER("Material Darker", true, FlatMTMaterialDarkerIJTheme::new), //
    MATERIAL_OCEANIC("Material Oceanic", true, FlatMTMaterialOceanicIJTheme::new), //
    MATERIAL_PALENIGHT("Material Palenight", true, FlatMTMaterialPalenightIJTheme::new), //
    MATERIAL_DEEP_OCEAN("Material Deep Ocean", true, FlatMTMaterialDeepOceanIJTheme::new), //
    FLAT_LIGHT("Flat Light", false, FlatLightLaf::new), //
    FLAT_DARK("Flat Dark", true, FlatDarkLaf::new);

    /**
     * The theme used if none is set in the preferences.
     */
    public static final GuiTheme DEFAULT = HM_LIGHT;

    private final String label;
    private final boolean dark;
    private final Supplier<LookAndFeel> lafSupplier;

    private GuiTheme( String label, boolean dark, Supplier<LookAndFeel> lafSupplier ) {
        this.label = label;
        this.dark = dark;
        this.lafSupplier = lafSupplier;
    }

    /**
     * @return the human readable name of the theme.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return <code>true</code> if this is a dark theme.
     */
    public boolean isDark() {
        return dark;
    }

    /**
     * @return a new instance of the look and feel of the theme.
     */
    public LookAndFeel createLookAndFeel() {
        return lafSupplier.get();
    }

    @Override
    public String toString() {
        return label + (dark ? " (dark)" : " (light)");
    }

    /**
     * Get the theme for a stored name.
     *
     * @param name the enum name of the theme, as stored in the preferences.
     * @return the theme or {@link #DEFAULT} if the name is <code>null</code>, empty or unknown.
     */
    public static GuiTheme fromName( String name ) {
        if (name != null) {
            for( GuiTheme theme : values() ) {
                if (theme.name().equalsIgnoreCase(name.trim())) {
                    return theme;
                }
            }
        }
        return DEFAULT;
    }
}
