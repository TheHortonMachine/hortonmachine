package org.hortonmachine.geoframe.core.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class TopologyNode {
	public int basinId;
	/**
	 * The value associated to this node (e.g. area, discharge.).
	 */
	public double value = Double.NaN;
	
	/**
	 * The accumulated value including all upstream nodes.
	 */
	public double accumulatedValue = Double.NaN;

	private TopologyNode downStreamNode;

	private List<TopologyNode> upStreamNodes = new CopyOnWriteArrayList<TopologyNode>();

	public TopologyNode(int basinId) {
		this.basinId = basinId;
	}

	public void setDownStreamNode(TopologyNode downStreamNode) {
		if (this.downStreamNode != null && !this.downStreamNode.equals(downStreamNode)) {
			throw new IllegalStateException("Downstream node already set for basin id: " + basinId);
		}
		this.downStreamNode = downStreamNode;
		// ensure bidirectional link
		if (!downStreamNode.upStreamNodes.contains(this)) {
			downStreamNode.upStreamNodes.add(this);
		}
	}

	public void addUpStreamNode(TopologyNode upStreamNode) {
		if (!upStreamNodes.contains(upStreamNode)) {
			upStreamNodes.add(upStreamNode);
		}
		// ensure bidirectional link
		upStreamNode.downStreamNode = this;
	}
	
	public boolean isLeafNode() {
		return upStreamNodes.isEmpty();
	}
	
    /**
     * Visit this node and all upstream nodes exactly once.
     *
     * @param visitor a function to process each node
     */
    public void visitUpstream(Consumer<TopologyNode> visitor) {
        visitUpstream(visitor, new HashSet<>());
    }

    private void visitUpstream(Consumer<TopologyNode> visitor, Set<TopologyNode> visited) {
        if (!visited.add(this)) {
            return; // already visited
        }
        // Visit self
        visitor.accept(this);

        // Visit all upstream nodes
        for (TopologyNode up : upStreamNodes) {
            up.visitUpstream(visitor, visited);
        }
    }


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TopologyNode other = (TopologyNode) obj;
		if (basinId != other.basinId)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + basinId;
		return result;
	}

	/**
	 * Get the root node of the topology given any node of the tree.
	 * 
	 * @param anyNode
	 * @return the root node
	 */
	public static TopologyNode getRootNode(TopologyNode anyNode) {
		TopologyNode currentNode = anyNode;
		while (currentNode.downStreamNode != null) {
			currentNode = currentNode.downStreamNode;
		}
		return currentNode;
	}

	/**
	 * Get all leaf nodes of the topology given the root node of the tree.
	 * 
	 * @param rootNode
	 * @return list of leaf nodes
	 */
	public static List<TopologyNode> getLeafNodes(TopologyNode rootNode) {
		List<TopologyNode> leafNodes = new CopyOnWriteArrayList<>();
		collectLeafNodes(rootNode, leafNodes);
		return leafNodes;
	}

	private static void collectLeafNodes(TopologyNode currentNode, List<TopologyNode> leafNodes) {
		if (currentNode.upStreamNodes.isEmpty()) {
			leafNodes.add(currentNode);
		} else {
			for (TopologyNode upNode : currentNode.upStreamNodes) {
				collectLeafNodes(upNode, leafNodes);
			}
		}
	}
	
    /**
     * Accumulate values downstream:
     *
     * @param root the root (most downstream) node
     */
    public static void accumulateDownstream(TopologyNode root) {
        // Optional but helpful: reset previous accumulated values
        resetAccumulated(root, new HashSet<>());

        // Compute new accumulated values
        accumulateRecursive(root, new HashSet<>());
    }

    private static double accumulateRecursive(TopologyNode node,
                                             Set<TopologyNode> visited) {
        if (!visited.add(node)) {
            return 0;
        }

        double sum = node.value;

        // Add accumulated sums from upstream nodes
        for (TopologyNode upstream : node.upStreamNodes) {
            sum += accumulateRecursive(upstream, visited);
        }

        // Store final accumulated result
        node.accumulatedValue = sum;
        return sum;
    }

    private static void resetAccumulated(TopologyNode node,
                                         Set<TopologyNode> visited) {
        if (!visited.add(node)) {
            return;
        }

        node.accumulatedValue = 0;  // clear old values

        for (TopologyNode up : node.upStreamNodes) {
            resetAccumulated(up, visited);
        }
    }


	/**
	 * Create a simple ASCII tree of the topology starting from the given root.
	 * Upstream nodes are printed as children.
	 *
	 * @param root root node of the topology
	 * @return a multiline String representing the topology
	 */
	public static String toAsciiTree(TopologyNode root) {
		if (root == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Set<TopologyNode> visited = new HashSet<>();
		visited.add(root);

		// print root
		sb.append(root.basinId);
		if (!root.isLeafNode() && !Double.isNaN(root.accumulatedValue)) {
			sb.append(" (").append(root.accumulatedValue).append(")");
		}
		if (!Double.isNaN(root.value)) {
			sb.append(" [").append(root.value).append("]");
		}
		sb.append('\n');

		// print children (upstream nodes)
		List<TopologyNode> children = root.upStreamNodes;
		for (int i = 0; i < children.size(); i++) {
			TopologyNode child = children.get(i);
			boolean isLast = (i == children.size() - 1);
			buildAsciiTree(child, "", isLast, visited, sb);
		}

		return sb.toString();
	}

	private static void buildAsciiTree(TopologyNode node, String prefix, boolean isLast, Set<TopologyNode> visited,
			StringBuilder sb) {
		if (!visited.add(node)) {
			// cycle protection – shouldn't happen for a real tree, but just in case
			sb.append(prefix).append(isLast ? "└── " : "├── ").append("[cycle to basin ").append(node.basinId)
					.append("]\n");
			return;
		}

		sb.append(prefix).append(isLast ? "└── " : "├── ").append(node.basinId);

		if (!node.isLeafNode() && !Double.isNaN(node.accumulatedValue)) {
			sb.append(" (").append(node.accumulatedValue).append(")");
		}
		if (!Double.isNaN(node.value)) {
			sb.append(" [").append(node.value).append("]");
		}
		sb.append('\n');

		List<TopologyNode> children = node.upStreamNodes;
		for (int i = 0; i < children.size(); i++) {
			TopologyNode child = children.get(i);
			boolean childIsLast = (i == children.size() - 1);
			String childPrefix = prefix + (isLast ? "    " : "│   ");
			buildAsciiTree(child, childPrefix, childIsLast, visited, sb);
		}
	}
	
	

	public static void main(String[] args) {

		// Create nodes
		TopologyNode n1 = new TopologyNode(1);
		n1.value = 10.0;
		TopologyNode n2 = new TopologyNode(2);
		n2.value = 20.0;
		TopologyNode n3 = new TopologyNode(3);
		n3.value = 30.0;
		TopologyNode n4 = new TopologyNode(4);
		n4.value = 40.0;
		TopologyNode n5 = new TopologyNode(5);
		n5.value = 50.0;

		// Build topology
		// 1 and 2 -> 3
		n1.setDownStreamNode(n3);
		n2.setDownStreamNode(n3);

		// 3 and 4 -> 5
		n3.setDownStreamNode(n5);
		n4.setDownStreamNode(n5);
		
		TopologyNode rootNode = TopologyNode.getRootNode(n1);
		accumulateDownstream(rootNode);

		// Print ASCII tree starting from any node (e.g. n2)
		System.out.println(TopologyNode.toAsciiTree(rootNode));
	}
}