package org.hortonmachine.geoframe.core.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


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

    /**
     * Visit all nodes in the topology, starting from
     * all leaf nodes and going downstream toward the root.
     * 
     * Each node is visited exactly once.
     * A downstream node is only visited after *all* its upstream nodes
     * have been visited.
     *
     * @param visitor function to apply to each visited node
     */
    public void visitDownstreamFromLeaves(Consumer<TopologyNode> visitor) {
        if (visitor == null) {
            return;
        }

        TopologyNode root = getRootNode(this);

        // Collect all nodes in this tree
        Set<TopologyNode> allNodes = new HashSet<>();
        collectAllUpstreamRecursive(root, allNodes);

        // Compute number of upstream nodes for each node (indegree)
        Map<TopologyNode, Integer> upstreamRemaining = new HashMap<>();
        for (TopologyNode n : allNodes) {
            upstreamRemaining.put(n, n.upStreamNodes.size());
        }

        // Queue initialized with all leaves (upstream count = 0)
        Deque<TopologyNode> queue = new ArrayDeque<>();
        for (TopologyNode n : allNodes) {
            if (n.upStreamNodes.isEmpty()) {
                queue.add(n);
            }
        }

        Set<TopologyNode> visited = new HashSet<>();

        // Kahn-style topological traversal
        while (!queue.isEmpty()) {
            TopologyNode node = queue.removeFirst();
            if (!visited.add(node)) {
                continue;
            }

            // Visit the node (safe: all upstream nodes already visited)
            visitor.accept(node);

            // Move to downstream node
            TopologyNode down = node.downStreamNode;
            if (down != null) {
                int newCount = upstreamRemaining.get(down) - 1;
                upstreamRemaining.put(down, newCount);

                // If downstream has no remaining upstream nodes, it can be visited
                if (newCount == 0) {
                    queue.addLast(down);
                }
            }
        }
    }

    /**
     * Collect all nodes upstream of (and including) a given node.
     * 
     * @param node the starting node
     * @param nodes set to collect nodes into
     */
    public static void collectAllUpstreamRecursive(TopologyNode node, Set<TopologyNode> nodes) {
        if (!nodes.add(node)) {
            return;
        }
        for (TopologyNode up : node.upStreamNodes) {
            collectAllUpstreamRecursive(up, nodes);
        }
    }

    /**
     * Parallel version of the downstream visit.
	 * 
	 * A node is only visited after all its upstream nodes have been visited.
	 * 
	 * @param numOfThreads number of threads to use or null, to use all available processors.
	 * @param visitor function to apply to each visited node. This needs to be thread-safe.
     */
    public void visitDownstreamFromLeavesParallel(Integer numOfThreads, Consumer<TopologyNode> visitor) {
        if (visitor == null) {
            return;
        }

        TopologyNode root = getRootNode(this);

        Set<TopologyNode> allNodes = new HashSet<>();
        collectAllUpstreamRecursive(root, allNodes);

        // For each node, track how many upstream nodes are still pending
        Map<TopologyNode, AtomicInteger> remainingUpstream = new HashMap<>();
        for (TopologyNode n : allNodes) {
            remainingUpstream.put(n, new AtomicInteger(n.upStreamNodes.size()));
        }

        // Thread pool and latch to wait for completion
        int nThreads = Math.max(1, numOfThreads != null ? numOfThreads : Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        CountDownLatch latch = new CountDownLatch(allNodes.size());

        // Submit all leaves (no upstreams) as starting tasks
        for (TopologyNode n : allNodes) {
            if (n.upStreamNodes.isEmpty()) {
                submitNodeTask(n, visitor, remainingUpstream, executor, latch);
            }
        }

        // Wait for all tasks to finish
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for parallel visit to finish", e);
        } finally {
            executor.shutdown();
        }
    }

    private static void submitNodeTask(
            TopologyNode node,
            Consumer<TopologyNode> visitor,
            Map<TopologyNode, AtomicInteger> remainingUpstream,
            ExecutorService executor,
            CountDownLatch latch) {

        executor.submit(() -> {
            try {
                // At this point, all upstream nodes have finished their visitor
                visitor.accept(node);
            } finally {
                latch.countDown();

                // Notify downstream node that one upstream has completed
                TopologyNode down = node.downStreamNode;
                if (down != null) {
                    AtomicInteger counter = remainingUpstream.get(down);
                    if (counter != null) {
                        // When this reaches zero, all upstreams are done -> schedule downstream
                        if (counter.decrementAndGet() == 0) {
                            submitNodeTask(down, visitor, remainingUpstream, executor, latch);
                        }
                    }
                }
            }
        });
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

		// Create 10 nodes
	    TopologyNode n1 = new TopologyNode(1);  n1.value = 10;
	    TopologyNode n2 = new TopologyNode(2);  n2.value = 20;
	    TopologyNode n3 = new TopologyNode(3);  n3.value = 30;
	    TopologyNode n4 = new TopologyNode(4);  n4.value = 40;
	    TopologyNode n5 = new TopologyNode(5);  n5.value = 50;
	    TopologyNode n6 = new TopologyNode(6);  n6.value = 60;
	    TopologyNode n7 = new TopologyNode(7);  n7.value = 70;
	    TopologyNode n8 = new TopologyNode(8);  n8.value = 80;
	    TopologyNode n9 = new TopologyNode(9);  n9.value = 90;
	    TopologyNode n10 = new TopologyNode(10); n10.value = 100;
	    TopologyNode n11 = new TopologyNode(11); n11.value = 110; // final outlet

	    // upstream connections
	    n1.setDownStreamNode(n4);
	    n2.setDownStreamNode(n5);
	    n5.setDownStreamNode(n4);

	    n4.setDownStreamNode(n6);
	    n7.setDownStreamNode(n6);

	    n6.setDownStreamNode(n9);
	    n10.setDownStreamNode(n8);
	    n8.setDownStreamNode(n3);
	    n9.setDownStreamNode(n3);

	    n3.setDownStreamNode(n11);
		
		TopologyNode rootNode = TopologyNode.getRootNode(n1);
		accumulateDownstream(rootNode);

		// Print ASCII tree starting from any node (e.g. n2)
		System.out.println(TopologyNode.toAsciiTree(rootNode));
		
		// now walk downstream from leaves
		System.out.println("Visiting downstream from leaves:");
		rootNode.visitDownstreamFromLeaves(node -> {
			System.out.println("Visited node basinId=" + node.basinId + ", value=" + node.value + ", accumulatedValue=" + node.accumulatedValue);
		});
		
		System.out.println("Visiting downstream from leaves in parallel:");
		rootNode.visitDownstreamFromLeavesParallel(5, n -> {
		    // This lambda runs in parallel for independent nodes,
		    // but always after all upstream nodes are done.
		    System.out.println(
		        "Thread " + Thread.currentThread().getName() +
		        " visiting basin " + n.basinId +
		        " value=" + n.value +
		        " acc=" + n.accumulatedValue
		    );
		});
	}
}