package org.hortonmachine.gears.io.stac;

import org.hortonmachine.gears.io.stac.assets.HMStacAssetHandlers;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An asset from a stac item.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMStacAsset {

	private String id;
	private String title;
	private String type;
	private String nonValidReason;
	private boolean isValid = true;

	private IHMStacAssetHandler handler;
	private JsonNode assetNode;

	public HMStacAsset(String id, JsonNode assetNode) {
		this.id = id;
		this.assetNode = assetNode;
		if (assetNode.has("title")) {
			title = assetNode.get("title").textValue();
		}
		JsonNode typeNode = assetNode.get("type");

		if (typeNode != null) {
			type = typeNode.textValue();
//            isAcceptedType = HMStacUtils.ACCEPTED_TYPES.contains(type.toLowerCase().replace(" ", ""));
//        } else {
//            isAcceptedType = HMStacUtils.ACCEPTED_EXTENSIONS.contains(FilenameUtils.getExtension(assetUrl));
		}
		handler = HMStacAssetHandlers.getHandler(this);
		if (handler == null) {
			isValid = false;
			nonValidReason = "no handler found for type: " + type;
		}
	}

	public JsonNode getAssetNode() {
		return assetNode;
	}

	public IHMStacAssetHandler getHandler() {
		return handler;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("title = " + title).append("\n");
		sb.append("type = " + type).append("\n");
		sb.append("isValid = " + isValid).append("\n");
		if (!isValid) {
			sb.append("nonValidReason = " + nonValidReason).append("\n");
		}
		return sb.toString();
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}

	public boolean isValid() {
		return isValid;
	}

	public String getNonValidReason() {
		return nonValidReason;
	}

}
