package org.hortonmachine.gears.io.stac;

import org.hortonmachine.gears.io.stac.assets.HMStacAssetHandlers;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.locationtech.jts.geom.Envelope;

import tools.jackson.databind.JsonNode;

/**
 * An asset from a stac item.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
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
	private HMStacAccess access;
	private String storageEndpoint;

	public HMStacAsset(String id, JsonNode assetNode) {
		this(id, assetNode, null);
	}

	/**
	 * @param id the asset id.
	 * @param assetNode the asset json.
	 * @param access the access context used to read the asset (e.g. with authentication), can be null.
	 */
	public HMStacAsset(String id, JsonNode assetNode, HMStacAccess access) {
		this(id, assetNode, access, null);
	}

	/**
	 * @param id the asset id.
	 * @param assetNode the asset json.
	 * @param access the access context used to read the asset (e.g. with authentication), can be null.
	 * @param storageSchemes the <code>storage:schemes</code> of the item or collection (storage extension),
	 *          to which the <code>storage:refs</code> of the asset refer, can be null.
	 */
	public HMStacAsset(String id, JsonNode assetNode, HMStacAccess access, JsonNode storageSchemes) {
		this.id = id;
		this.access = access;
		this.assetNode = assetNode;
		this.storageEndpoint = findStorageEndpoint(assetNode, storageSchemes);
		if (assetNode.has("title")) {
			title = assetNode.get("title").stringValue(null);
		}
		JsonNode typeNode = assetNode.get("type");
		if (typeNode != null) {
			type = typeNode.stringValue(null);
//            isAcceptedType = HMStacUtils.ACCEPTED_TYPES.contains(type.toLowerCase().replace(" ", ""));
//        } else {
//            isAcceptedType = HMStacUtils.ACCEPTED_EXTENSIONS.contains(FilenameUtils.getExtension(assetUrl));
		}
		
		JsonNode epsgNode = assetNode.get("proj:code");
		if (epsgNode != null) {
			String epsgCode = epsgNode.stringValue(null);
			if (epsgCode != null && epsgCode.toLowerCase().startsWith("epsg:")) {
				// Corner case when proj:code is defined, but it's null :(
				// Example: https://stac.terrascope.be/collections/lcfm-lcm-10/items/LCFM_LCM-10_V100_2020_S57W072_MAP for the asset "Preview" before applying the predicate
				try {
					epsg = Integer.parseInt(epsgCode.substring(5));
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		if (epsg == null) {
			// projection extension v1.x: integer code
			JsonNode projEpsgNode = assetNode.get("proj:epsg");
			if (projEpsgNode != null && projEpsgNode.canConvertToInt()) {
				epsg = projEpsgNode.asInt();
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

	/**
	 * @return the access context used to read the asset or null.
	 */
	public HMStacAccess getAccess() {
		return access;
	}

	/**
	 * The endpoint of the S3 compatible service holding the asset, as declared by the storage extension
	 * (the <code>platform</code> of the first <code>custom-s3</code> scheme the asset refers to, preferring
	 * the ones that are not requester pays).
	 *
	 * @return the endpoint url (e.g. <code>https://eodata.dataspace.copernicus.eu</code>) or null if not declared.
	 */
	public String getStorageEndpoint() {
		return storageEndpoint;
	}

	private static String findStorageEndpoint(JsonNode assetNode, JsonNode storageSchemes) {
		JsonNode refs = assetNode.get("storage:refs");
		if (storageSchemes == null || !storageSchemes.isObject() || refs == null || !refs.isArray()) {
			return null;
		}
		String requesterPaysEndpoint = null;
		for (JsonNode ref : refs) {
			JsonNode scheme = storageSchemes.get(ref.asString(""));
			if (scheme == null || !"custom-s3".equals(scheme.path("type").asString(""))) {
				continue;
			}
			String platform = scheme.path("platform").asString(null);
			// templated platforms (e.g. https://{bucket}.example.com) are not handled
			if (platform == null || !platform.startsWith("http") || platform.contains("{")) {
				continue;
			}
			if (!scheme.path("requester_pays").asBoolean(false)) {
				return platform;
			}
			if (requesterPaysEndpoint == null) {
				requesterPaysEndpoint = platform;
			}
		}
		return requesterPaysEndpoint;
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

	/**
	 * Set the epsg of the asset, for example when read from the file since the catalog doesn't declare it.
	 *
	 * @param epsg the epsg code.
	 */
	public void setEpsg(Integer epsg) {
		this.epsg = epsg;
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
