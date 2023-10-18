package org.hortonmachine.gears.io.wcs;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlHelper {

    public interface XmlVisitor {
        boolean checkElementName(String name);

        void visit(Node node);
    }

    private Node rootNode;

    private XmlHelper(Object source) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = null;
            if (source instanceof File) {
                document = dBuilder.parse((File) source);
            } else if (source instanceof String) {
                document = dBuilder.parse((String) source);
            } else if (source instanceof InputStream) {
                document = dBuilder.parse((InputStream) source);
            } else {
                throw new IllegalArgumentException("Source must be a File, String or Inputstream");
            }
            document.getDocumentElement().normalize();
            rootNode = document.getDocumentElement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Node getRootNode() {
        return rootNode;
    }

    public static XmlHelper fromFile(File xmlFile) {
        return new XmlHelper(xmlFile);
    }

    public static XmlHelper fromString(String xmlString) {
        return new XmlHelper(xmlString);
    }

    public static XmlHelper fromStream(InputStream xmlStream) {
        return new XmlHelper(xmlStream);
    }

    public static void apply(Node node, XmlVisitor visitor) {
        if (visitor != null && visitor.checkElementName(node.getNodeName())) {
            visitor.visit(node);
        }
        Node child = node.getFirstChild();
        while (child != null) {
            apply(child, visitor);
            child = child.getNextSibling();
        }
    }

    /**
     * Finds the first child node with the given name and returns its value.
     * 
     * @param node          the parent node to search in
     * @param lowerCaseName the name of the child node to find
     * @return the value of the first child node with the given name, or null if not
     *         found
     */
    public static String findFirstTextInChildren(Node node, String lowerCaseName) {
        lowerCaseName = lowerCaseName.toLowerCase();
        Node child = node.getFirstChild();
        while (child != null) {
            String nodeName = child.getNodeName();
            if (nodeName.toLowerCase().endsWith(lowerCaseName)) {
                String value = child.getNodeValue();
                if (value != null) {
                    return value.trim();
                } else {
                    // try to get first valid child node value
                    NodeList childNodes = child.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node childNode = childNodes.item(i);
                        String childNodeValue = childNode.getNodeValue();
                        if (childNodeValue != null) {
                            return childNodeValue.trim();
                        }
                    }

                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    /**
     * Finds all text values of child nodes with a given name (case-insensitive) in
     * the given node.
     * If a child node has no text value, it tries to get the first valid child node
     * value.
     *
     * @param node          the node to search in
     * @param lowerCaseName the name of the child nodes to search for
     *                      (case-insensitive)
     * @return a list of all text values of the matching child nodes
     */
    public static List<String> findAllTextsInChildren(Node node, String lowerCaseName) {
        lowerCaseName = lowerCaseName.toLowerCase();
        List<String> values = new ArrayList<>();
        Node child = node.getFirstChild();
        while (child != null) {
            String nodeName = child.getNodeName();
            if (nodeName.toLowerCase().contains(lowerCaseName)) {
                String value = child.getNodeValue();
                if (value != null) {
                    values.add(value.trim());
                } else {
                    // try to get first valid child node value
                    NodeList childNodes = child.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node childNode = childNodes.item(i);
                        String childNodeValue = childNode.getNodeValue();
                        if (childNodeValue != null) {
                            values.add(childNodeValue.trim());
                        }
                    }
                }
            }
            child = child.getNextSibling();
        }
        return values;
    }

    public static Node findNode(Node node, String lowerCaseName) {
        lowerCaseName = lowerCaseName.toLowerCase();

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            String nodeName = childNode.getNodeName();
            if (nodeName.toLowerCase().endsWith(lowerCaseName)) {
                return childNode;
            } else {
                Node foundNode = findNode(childNode, lowerCaseName);
                if (foundNode != null) {
                    return foundNode;
                }
            }
        }

        // Node child = node.getFirstChild();
        // while (child != null) {
        //     String nodeName = child.getNodeName();
        //     if (nodeName.toLowerCase().contains(lowerCaseName)) {
        //         return child;
        //     }
        //     child = child.getNextSibling();
        // }
        return null;
    }

    public static String findAttribute(Node node, String lowerCaseName) {
        lowerCaseName = lowerCaseName.toLowerCase();
        if (node.hasAttributes()) {
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().toLowerCase().contains(lowerCaseName)) {
                    return attribute.getNodeValue();
                }
            }
        }
        return null;
    }

    public void printTree() {
        printTree(rootNode, 0);
    }

    /**
     * Browse the XML tree and print out the nodes or optionally use a visitor.
     * 
     * @param node    The node to start browsing from
     * @param level   The level of indentation
     * @param visitor The visitor to use
     */
    public void printTree(Node node, int level) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            System.out.println(getIndentation(level) + "Element: " + node.getNodeName());
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attribute = attributes.item(i);
                    System.out.println(getIndentation(level + 1) + "Attribute: " + attribute.getNodeName() + " = "
                            + attribute.getNodeValue());
                }
            }
        } else if (node.getNodeType() == Node.TEXT_NODE && !node.getNodeValue().trim().isEmpty()) {
            System.out.println(getIndentation(level) + "Text: " + node.getNodeValue());
        }

        Node child = node.getFirstChild();
        while (child != null) {
            printTree(child, level + 1);
            child = child.getNextSibling();
        }
    }

    private Map<String, Object> getSubElements(Node node) {
        Node child = node.getFirstChild();
        Map<String, Object> subElements = new HashMap<>();
        while (child != null) {
            String nodeName = node.getNodeName();
            String value = node.getNodeValue();

            subElements.put(nodeName, value);

            child = child.getNextSibling();
        }
        return subElements;
    }

    public String getIndentation(int level) {
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indentation.append("  "); // Use two spaces for each level of indentation
        }
        return indentation.toString();
    }

}
