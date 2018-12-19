package org.hortonmachine.style;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.colors.DefaultTables;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.style.RasterSymbolizerWrapper;
import org.hortonmachine.gui.utils.GuiUtilities;

public class RasterStyleController extends RasterStyleView {
	private static final String DEFAULT_NUMFORMAT = "0.00";
	private static final String CUSTOM_RASTER_STYLES_KEY = "CUSTOM_RASTER_STYLES";
	private RasterSymbolizerWrapper symbolizer;
	private GridCoverage2D raster;

	public RasterStyleController(RasterSymbolizerWrapper symbolizer, GridCoverage2D raster,
			MainController mainController) {
		this.symbolizer = symbolizer;
		this.raster = raster;

		numFormatField.setText(DEFAULT_NUMFORMAT);
		novalueTextfield.setText(HMConstants.doubleNovalue + "");
		interpolatedCheckbox.setSelected(true);

		colortablesCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String colorTableText = customStyleArea.getText();
				if (colorTableText.trim().length() == 0 && colortablesCombo != null) {
					Object selectedItem = colortablesCombo.getSelectedItem();
					if (selectedItem != null) {
						String colorTableName = selectedItem.toString();
						if (colorTableName.trim().length() > 0) {
							String tableString = new DefaultTables().getTableString(colorTableName);
							customStyleArea.setText(tableString);
						}
					}
				}
			}
		});

		applyTableButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyStyle();
			}
		});

		customStyleButton.addActionListener(e -> {
			String name = GuiUtilities.showInputDialog(this, "Enter a name for the colortable",
					EColorTables.greyscale.name());
			if (name == null || name.trim().length() == 0) {
				return;
			}
			String colorTableText = customStyleArea.getText();

			String tableString = new DefaultTables().getTableString(name);
			if (tableString != null) {
				int answer = JOptionPane.showConfirmDialog(this,
						"A colortable with that name already exists. Overwrite it?", "WARNING",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (answer == JOptionPane.NO_OPTION) {
					return;
				}
			}
			DefaultTables.addRuntimeTable(name, colorTableText);

			String[] tableNames = DefaultTables.getTableNames();
			String[] tableNames2 = new String[tableNames.length + 1];
			System.arraycopy(tableNames, 0, tableNames2, 1, tableNames.length);
			tableNames2[0] = "";
			colortablesCombo.setModel(new DefaultComboBoxModel<String>(tableNames2));
			colortablesCombo.setSelectedItem(name);

			applyStyle();
		});

		setCombos();
	}

	private void setCombos() {
		Object selectedColor = colortablesCombo.getSelectedItem();
		Object transparencyColor = opacityCombo.getSelectedItem();

		String[] tableNames = DefaultTables.getTableNames();
		String[] tableNames2 = new String[tableNames.length + 1];
		System.arraycopy(tableNames, 0, tableNames2, 1, tableNames.length);
		tableNames2[0] = "";
		colortablesCombo.setModel(new DefaultComboBoxModel<String>(tableNames2));

		Integer[] transparency = new Integer[] { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
		opacityCombo.setModel(new DefaultComboBoxModel<Integer>(transparency));
		opacityCombo.setSelectedIndex(transparency.length - 1);

		colortablesCombo.setSelectedItem(selectedColor);
		opacityCombo.setSelectedItem(transparencyColor);
	}

	private void applyStyle() {
		String colorTableName = colortablesCombo.getSelectedItem().toString();

		String novalueText = novalueTextfield.getText();
		Double novalue = null;
		if (novalueText.trim().length() > 0) {
			try {
				novalue = Double.parseDouble(novalueText);
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

		int opacity = 100;
		if (opacityCombo.getSelectedItem() != null)
			opacity = (Integer) opacityCombo.getSelectedItem();
		opacity = (int) (opacity * 255 / 100.0);

		String numFormatPattern = numFormatField.getText();
		if (numFormatPattern.trim().length() == 0) {
			numFormatPattern = DEFAULT_NUMFORMAT;
		}
		try {
			Style style = RasterStyleUtilities.createStyleForColortable(colorTableName, min, max, opacity);
			Symbolizer newRasterSymbolizer = style.featureTypeStyles().get(0).rules().get(0).getSymbolizers()[0];
			symbolizer.setRasterSymbolizer((RasterSymbolizer) newRasterSymbolizer);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

}