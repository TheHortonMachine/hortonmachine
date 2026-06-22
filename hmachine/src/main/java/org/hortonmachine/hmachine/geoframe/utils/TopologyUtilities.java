package org.hortonmachine.hmachine.geoframe.utils;

import java.util.HashMap;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.hmachine.geoframe.core.TopologyNode;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.TopologySchema.TopologyField;

public class TopologyUtilities {

	public static TopologyNode getRootNodeFromDb(ADb db) throws Exception {
		QueryResult result = db.getTableRecordsMapFromRawSql("select * from " + GeoFrameSimpleTable.TOPOLOGY.tableName(), -1);
		int fromIndex = result.names.indexOf(TopologyField.UPPSTREAM_BASIN.columnName());
		int toIndex = result.names.indexOf(TopologyField.DOWNSTREAM_BASIN.columnName());

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