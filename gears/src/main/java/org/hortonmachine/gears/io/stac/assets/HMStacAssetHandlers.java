package org.hortonmachine.gears.io.stac.assets;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.handlers.GeojsonHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.GeopackageVectorHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.GeotiffHandler;

public final class HMStacAssetHandlers {

    private static final List<Class<? extends IHMStacAssetHandler>> HANDLERS = new ArrayList<>();

    static {
        // Built-in handlers
        HANDLERS.add(GeopackageVectorHandler.class);
        HANDLERS.add(GeotiffHandler.class);
        HANDLERS.add(GeojsonHandler.class);
    }

    private HMStacAssetHandlers() {}

    /**
     * Register a new external asset handler.
     * 
     * @param handler the handler class to register.
     */
    public static void register(Class<? extends IHMStacAssetHandler> handler) {
        HANDLERS.add(handler);
    }

    /**
     * Get the appropriate handler for the given asset or null if none is found.
     * 
     * @param asset the asset to get the handler for.
     * @return the handler or null.
     */
    public static IHMStacAssetHandler getHandler(HMStacAsset asset) {
		for (Class<? extends IHMStacAssetHandler> handlerClass : HANDLERS) {
			try {
				IHMStacAssetHandler handler = handlerClass.getDeclaredConstructor().newInstance();
				handler.initialize(asset);
				if (handler.supports()) {
					return handler;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
		 
	}
}
