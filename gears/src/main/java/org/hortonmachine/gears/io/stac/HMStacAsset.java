package org.hortonmachine.gears.io.stac;

import org.hortonmachine.gears.io.stac.assets.HMStacAssetHandlers;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.locationtech.jts.geom.Envelope;

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
	
	private Integer epsg;
	private Envelope envelope;

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
		
		JsonNode epsgNode = assetNode.get("proj:code");
		if (epsgNode != null) {
			String epsgCode = epsgNode.textValue();
			if (epsgCode.toLowerCase().startsWith("epsg:")) {
				try {
					epsg = Integer.parseInt(epsgCode.substring(5));
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		
		JsonNode bboxNode = assetNode.get("proj:bbox");
		if (bboxNode != null && bboxNode.isArray() && bboxNode.size() == 4) {
			double minX = bboxNode.get(0).asDouble();
			double minY = bboxNode.get(1).asDouble();
			double maxX = bboxNode.get(2).asDouble();
			double maxY = bboxNode.get(3).asDouble();
			envelope = new Envelope(minX, maxX, minY, maxY);
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
	
	public Integer getEpsg() {
		return epsg;
	}
	
	public Envelope getEnvelope() {
		return envelope;
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
