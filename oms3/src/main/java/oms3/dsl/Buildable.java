package oms3.dsl;

/**
 * Anything that can be built using a GenericBuilder
 * 
 * @author od
 */
public interface Buildable {

    /**
     * Create a node.
     * 
     * @param name the node name
     * @param value  the value
     * @return a new Subnode
     */
    Buildable create(Object name, Object value);

    /**
     * Default LEAF
     */
    public static Buildable LEAF = new Buildable() {

        @Override
        public Buildable create(Object name, Object value) {
            throw new Error("Cannot add to a leaf node.");
        }
    };
}
