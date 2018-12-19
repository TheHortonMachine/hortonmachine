package org.hortonmachine.gears.utils.style;

import java.util.List;

import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Symbolizer;

/**
 * A wrapper for a {@link RasterSymbolizer} to ease interaction with gui.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RasterSymbolizerWrapper extends SymbolizerWrapper {

    private RasterSymbolizer rasterSymbolizer;

    public RasterSymbolizerWrapper( Symbolizer symbolizer, RuleWrapper parent ) {
        super(symbolizer, parent);

        rasterSymbolizer = (RasterSymbolizer) symbolizer;
    }
    
    public RasterSymbolizer getRasterSymbolizer() {
        return rasterSymbolizer;
    }

	public void setRasterSymbolizer(RasterSymbolizer newRasterSymbolizer) {
		rasterSymbolizer = newRasterSymbolizer;
		
		RuleWrapper parent = getParent();
		List<Symbolizer> symbolizers = parent.getRule().symbolizers();
		symbolizers.clear();
		symbolizers.add(rasterSymbolizer);
	}
}
