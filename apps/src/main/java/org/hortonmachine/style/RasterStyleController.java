package org.hortonmachine.style;

import static org.hortonmachine.gears.utils.style.StyleUtilities.ff;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.ShadedRelief;
import org.geotools.styling.ShadedReliefImpl;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.colors.DefaultTables;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.style.RasterSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

@SuppressWarnings({"unchecked", "serial"})
public class RasterStyleController extends RasterStyleView {
    private RasterSymbolizerWrapper symbolizer;
    private GridCoverage2D raster;
    private MainController mainController;

    public RasterStyleController( RasterSymbolizerWrapper symbolizer, GridCoverage2D raster, MainController mainController ) {
        this.symbolizer = symbolizer;
        this.raster = raster;
        this.mainController = mainController;

        String tableName = symbolizer.getParent().getName();
        if (tableName == null) {
            tableName = "other table";
        }

        _novalueTextfield.setText(HMConstants.doubleNovalue + "");

        _colortablesCombo.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                Object selectedItem = _colortablesCombo.getSelectedItem();
                if (selectedItem != null) {
                    String colorTableName = selectedItem.toString();
                    if (colorTableName.trim().length() > 0) {
                        String tableString = new DefaultTables().getTableString(colorTableName);
                        _customStyleArea.setText(tableString);
                    }
                }
            }
        });

        _applyTableButton.addActionListener(new ActionListener(){
            public void actionPerformed( ActionEvent e ) {
                applyStyle();
            }
        });

        _customStyleButton.addActionListener(e -> {
            String name = GuiUtilities.showInputDialog(this, "Enter a name for the colortable", EColorTables.greyscale.name());
            if (name == null || name.trim().length() == 0) {
                return;
            }
            String colorTableText = _customStyleArea.getText();

            String tableString = new DefaultTables().getTableString(name);
            if (tableString != null) {
                int answer = JOptionPane.showConfirmDialog(this, "A colortable with that name already exists. Overwrite it?",
                        "WARNING", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            DefaultTables.addRuntimeTable(name, colorTableText);

            String[] tableNames = DefaultTables.getTableNames();
            String[] tableNames2 = new String[tableNames.length + 1];
            System.arraycopy(tableNames, 0, tableNames2, 1, tableNames.length);
            tableNames2[0] = "";
            _colortablesCombo.setModel(new DefaultComboBoxModel<String>(tableNames2));
            _colortablesCombo.setSelectedItem(name);

            applyStyle();
        });

        String[] tableNames = DefaultTables.getTableNames();
        String[] tableNames2 = new String[tableNames.length + 1];
        System.arraycopy(tableNames, 0, tableNames2, 1, tableNames.length);
        tableNames2[0] = "";
        _colortablesCombo.setModel(new DefaultComboBoxModel<String>(tableNames2));
        _colortablesCombo.setSelectedItem(tableName);

        double opacity = symbolizer.getOpacity();
        int opacityInt = (int) (opacity * 100);

        Integer[] transparency = new Integer[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        _opacityCombo.setModel(new DefaultComboBoxModel<Integer>(transparency));
        _opacityCombo.setSelectedItem(opacityInt);

        ShadedRelief shadedRelief = symbolizer.getRasterSymbolizer().getShadedRelief();
        if (shadedRelief != null) {
            Expression reliefFactor = shadedRelief.getReliefFactor();
            Double rfDouble = reliefFactor.evaluate(null, Double.class);
            _reliefFactorField.setText(rfDouble.toString());
            _shadedReliefCheck.setSelected(true);
        } else {
            _reliefFactorField.setText("1");
            _shadedReliefCheck.setSelected(false);
        }

    }

    private void applyStyle() {
        String colorTableName = _colortablesCombo.getSelectedItem().toString();

        String novalueText = _novalueTextfield.getText();
        if (novalueText.trim().length() > 0) {
            try {
                Double.parseDouble(novalueText);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        try {
            double[] minMax = OmsRasterSummary.getMinMax(raster);
            min = minMax[0];
            max = minMax[1];
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        double opacity = 100;
        if (_opacityCombo.getSelectedItem() != null)
            opacity = (Integer) _opacityCombo.getSelectedItem();
        opacity = opacity / 100.0;

        boolean doShadedRelief = _shadedReliefCheck.isSelected();
        double rf = 2;
        String rfTStr = _reliefFactorField.getText();
        try {
            rf = Double.parseDouble(rfTStr);
        } catch (Exception e) {
        }
        try {
            Style style = RasterStyleUtilities.createStyleForColortable(colorTableName, min, max, opacity);
            Symbolizer newRasterSymbolizer = style.featureTypeStyles().get(0).rules().get(0).getSymbolizers()[0];

            if (doShadedRelief) {
                ShadedRelief sr = new ShadedReliefImpl();
                sr.setBrightnessOnly(false);
                Literal rfLiteral = StyleUtilities.ff.literal(rf);
                sr.setReliefFactor(rfLiteral);
                ((RasterSymbolizer) newRasterSymbolizer).setShadedRelief(sr);
            } else {
                ((RasterSymbolizer) newRasterSymbolizer).setShadedRelief(null);
            }

            symbolizer.setRasterSymbolizer((RasterSymbolizer) newRasterSymbolizer);
            symbolizer.getParent().setName(colorTableName);
            mainController.applyStyle();

        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}