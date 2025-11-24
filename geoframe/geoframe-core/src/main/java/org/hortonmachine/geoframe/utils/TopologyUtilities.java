package org.hortonmachine.geoframe.utils;

import java.util.HashMap;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.hmachine.utils.GeoframeUtils;

public class TopologyUtilities {

	public static TopologyNode getRootNodeFromDb(ADb db) throws Exception {
		QueryResult result = db.getTableRecordsMapFromRawSql("select * from " + GeoframeUtils.GEOFRAME_TOPOLOGY_TABLE,
				-1);
		int fromIndex = result.names.indexOf(GeoframeUtils.GEOFRAME_TOPOLOGY_FIELD_FROM);
		int toIndex = result.names.indexOf(GeoframeUtils.GEOFRAME_TOPOLOGY_FIELD_TO);

		HashMap<Integer, TopologyNode> topologyBasinsMap = new HashMap<>();
		for (Object[] row : result.data) {
			int fromBasinId = ((Number) row[fromIndex]).intValue();
			int toBasinId = ((Number) row[toIndex]).intValue();

			TopologyNode fromNode = topologyBasinsMap.get(fromBasinId);
			if (fromNode == null) {
				fromNode = new TopologyNode(fromBasinId);
				topologyBasinsMap.put(fromBasinId, fromNode);
			}
			if (toBasinId != 0) { // 0 is used to indicate no downstream basin
				TopologyNode toNode = topologyBasinsMap.get(toBasinId);
				if (toNode == null) {
					toNode = new TopologyNode(toBasinId);
					topologyBasinsMap.put(toBasinId, toNode);
				}
				fromNode.setDownStreamNode(toNode);
			}
		}
		TopologyNode rootNode = TopologyNode.getRootNode(topologyBasinsMap.values().stream().findFirst().get());
		return rootNode;
	}

}
