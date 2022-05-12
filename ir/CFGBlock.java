package ir;

import java.util.*;
import java.util.stream.Collectors;

public class CFGBlock {
    private int blockIndex;
    private List<Integer> children;
    private Set<CFGBlock> cfgChildren;
    private Set<CFGBlock> cfgParents;
    private Map<Integer, String> labels;

    private List<TACTerm> terms;

    private List<CFGBlock> doms;
    private List<CFGBlock> idoms;
    private List<CFGBlock> reverseDoms;
    private List<CFGBlock> reverseIDoms;
    private boolean visited;
    private boolean deleted;

    private FunctionSymbol currentScope;
    private BlockType blockType;

    public CFGBlock repeatCompareBlock = null;
    public CFGBlock repeatElseBlock = null;
    public CFGBlock thenBlock = null;
    public CFGBlock elseBlock = null;
    public CFGBlock joinBlock = null;
    public CFGBlock whileThenBlock = null;
    public CFGBlock whileElseBlock = null;

    public CFGBlock(int index, FunctionSymbol scope) {
        blockIndex = index;
        children = new ArrayList<>();
        cfgChildren = new HashSet<>();
        cfgParents = new HashSet<>();
        labels = new HashMap<>();
        terms = new ArrayList<>();

        doms = new ArrayList<>();
        idoms = new ArrayList<>();
        reverseDoms = new ArrayList<>();
        reverseIDoms = new ArrayList<>();
        visited = false;
        deleted = false;

        currentScope = scope;
        blockType = BlockType.COMMON;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }

    public void setCurrentScope(FunctionSymbol currentScope) {
        this.currentScope = currentScope;
    }

    public FunctionSymbol getCurrentScope() {
        return currentScope;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void insertChildren(int block) {
        this.children.add(block);
    }

    public void insertCFGParent(CFGBlock block) {
        this.cfgParents.add(block);
    }

    public void insertLabel(Integer blockId, String label) {
        this.labels.put(blockId, label == null ? "" : label);
    }

    public void insertCFGChildren(CFGBlock cfgBlock) {
        this.cfgChildren.add(cfgBlock);
    }

    public void insertTerm(TACTerm term) {
        this.terms.add(term);
    }

    public void insertDoms(CFGBlock cfgBlock) { this.doms.add(cfgBlock); }

    public void insertReverseDoms(CFGBlock cfgBlock) { this.reverseDoms.add(cfgBlock); }

    public void insertIDoms(CFGBlock cfgBlock) { this.idoms.add(cfgBlock); }

    public void insertReverseIDoms(CFGBlock cfgBlock) { this.reverseIDoms.add(cfgBlock); }

    public void removeCFGChildren(CFGBlock block) {
        this.cfgChildren.remove(block);
    }

    public void removeCFGParent(CFGBlock block) {
        this.cfgParents.remove(block);
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public Set<CFGBlock> getCfgChildren() {
        return cfgChildren;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public Set<CFGBlock> getCfgParents() {
        return cfgParents;
    }

    public List<TACTerm> getTerms() {
        return terms;
    }

    public List<CFGBlock> getDoms() {
        return doms;
    }

    public List<CFGBlock> getReverseDoms() {
        return reverseDoms;
    }

    public List<CFGBlock> getIdoms() {
        return idoms;
    }

    public List<CFGBlock> getReverseIDoms() {
        return reverseIDoms;
    }

    public Map<Integer, String> getLabels() {
        return labels;
    }

    public List<CFGBlock> getFuncCallBlocks() {
        List<CFGBlock> funcCalls = new ArrayList<>();
        for (TACTerm term : terms) {
            if (term.getOps().equals("CALL")) {
                assert (term.getFuncCall() != null) : "The function block " + term.getSrc() + " is not exist";
                funcCalls.add(term.getFuncCall());
            }
        }
        return funcCalls;
    }

    public String getConnections() {
        StringBuilder connections = new StringBuilder();
        for (TACTerm term : terms) {
            if (term.isDeleted())
                continue;
            if (term.getOps().equals("CALL")) {
                assert (term.getFuncCall() != null) : "The function block " + term.getSrc() + " is not exist";
                connections.append("BB").append(blockIndex).append(":").append(term.getLineCount()).append("-> BB").append(term.getFuncCall().getBlockIndex()).append("[label=\"call\",color=\"red\"];\n");
            }

        }
        for (CFGBlock child: cfgChildren) {
            connections.append("BB").append(blockIndex).append("-> BB").append(child.getBlockIndex()).append("[label=\"").append(labels.getOrDefault(child.getBlockIndex(), "")).append("\"];\n");
        }

        for (CFGBlock child: idoms) {
            connections.append("BB").append(blockIndex).append("-> BB").append(child.getBlockIndex()).append("[label=\"dominate\",color=\"blue\"];\n");
        }
//        for (CFGBlock child: reverseIDoms) {
//            connections.append("BB").append(blockIndex).append("-> BB").append(child.getBlockIndex()).append("[label=\"reverse-dominate\",color=\"green\"];\n");
//        }
        return connections.toString();
    }

    @Override
    public String toString() {
        if (CONFIG.SHOW_DELETED)
            return "BB" + blockIndex + "[label=\"{ BB" + blockIndex + "|{" + terms.stream().map(TACTerm::toString)
                    .collect(Collectors.joining("|")) + "}}\"]\n";
        return "BB" + blockIndex + "[label=\"{ BB" + blockIndex + "|{" + terms.stream().filter(term->!term.isDeleted()).map(TACTerm::toString)
                .collect(Collectors.joining("|")) + "}}\"]\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CFGBlock)) return false;
        CFGBlock cfgBlock = (CFGBlock) o;
        return getBlockIndex() == cfgBlock.getBlockIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBlockIndex());
    }
}

