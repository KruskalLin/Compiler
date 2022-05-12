package parser;

import java.util.*;

public class ASTTreeNode {

    private final String symbol;
    private String value;
    private final List<ASTTreeNode> children;

    public ASTTreeNode(String node, String value) {
        this.symbol = node;
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTTreeNode node) {
        children.add(node);
    }

    public List<ASTTreeNode> getChildren() {
        return children;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) { this.value = value; }

    public void reverse() {
        Collections.reverse(this.children);
    }
}
