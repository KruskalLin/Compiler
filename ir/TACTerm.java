package ir;

import java.util.*;
import java.util.stream.Collectors;

public class TACTerm {
    private int lineCount;
    private String ops;
    private Variable src;
    private Variable dst;
    private List<Variable> dsts;
    private List<CFGBlock> phiSources;

    private CFGBlock funcCall;

    private final Type derivedType;

    // only use for analysis
    private boolean deleted;
    private boolean movingArray;

    private int outputRegister;
    private int srcRegister;
    private int dstRegister;
    private List<Integer> dstsRegister;

    // prevent phi2move have same lineCount and deem as same object
    private int uuid = 0;


    // for machine code use
    private String storeName;

    public TACTerm nextTerm = null;
    public int offset = 0;

    public TACTerm(int lineCount, String ops, String src, String dst, Type type) {
        this.lineCount = lineCount;
        this.ops = ops;
        this.src = src == null ? null : new Variable(src, 0);
        this.dst = dst == null ? null : new Variable(dst, 0);
        this.derivedType = type;
        this.deleted = false;
        this.srcRegister = -1;
        this.dstRegister = -1;
        this.outputRegister = -1;

        this.phiSources = new ArrayList<>();
        this.movingArray = false;
    }

    public TACTerm(int lineCount, String ops, Variable src, Variable dst, Type type) {
        this.lineCount = lineCount;
        this.ops = ops;
        this.src = src;
        this.dst = dst;
        this.derivedType = type;
        this.deleted = false;
        this.srcRegister = -1;
        this.dstRegister = -1;
        this.outputRegister = -1;

        this.phiSources = new ArrayList<>();
        this.movingArray = false;
    }

    public TACTerm(int lineCount, String ops, Variable src, Variable dst, Type type, String storeName) {
        this.lineCount = lineCount;
        this.ops = ops;
        this.src = src;
        this.dst = dst;
        this.derivedType = type;
        this.deleted = false;
        this.srcRegister = -1;
        this.dstRegister = -1;
        this.outputRegister = -1;

        this.phiSources = new ArrayList<>();
        this.movingArray = false;
        this.storeName = storeName;
    }

    public TACTerm(int lineCount, String ops, Variable src, Variable dst, Type type, int uuid) {
        this.lineCount = lineCount;
        this.ops = ops;
        this.src = src;
        this.dst = dst;
        this.derivedType = type;
        this.deleted = false;
        this.srcRegister = -1;
        this.dstRegister = -1;
        this.outputRegister = -1;

        this.phiSources = new ArrayList<>();
        this.movingArray = false;

        this.uuid = uuid;
    }


    public TACTerm(int lineCount, String ops, String src, List<String> dsts, Type type) {
        this.lineCount = lineCount;
        this.ops = ops;
        this.src = src == null ? null : new Variable(src, 0);
        this.dsts = dsts == null ? new ArrayList<>() : dsts.stream().map(d -> new Variable(d, 0)).collect(Collectors.toList());
        this.derivedType = type;
        this.deleted = false;
        this.srcRegister = -1;
        this.dstRegister = -1;
        this.dstsRegister = new ArrayList<>();
        this.outputRegister = -1;

        this.phiSources = new ArrayList<>();
        this.movingArray = false;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setMovingArray(boolean movingArray) {
        this.movingArray = movingArray;
    }

    public boolean isMovingArray() {
        return movingArray;
    }

    public void addPHISource(CFGBlock block) {
        this.phiSources.add(block);
    }

    public List<CFGBlock> getPhiSources() {
        return phiSources;
    }

    public void setPhiSources(List<CFGBlock> phiSources) {
        this.phiSources = phiSources;
    }

    public void setSrc(Variable src) {
        this.src = src;
    }

    public void setDst(Variable dst) {
        this.dst = dst;
    }

    public void setDsts(List<Variable> dsts) {
        this.dsts = dsts;
    }

    public void setDstsIndex(int index, int targetIndex) {
        this.dsts.get(index).setIndex(targetIndex);
    }

    public Variable getSrc() {
        return src;
    }

    public Variable getDst() {
        return dst;
    }

    public List<Variable> getDsts() {
        return dsts;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public void setOps(String ops) {
        this.ops = ops;
    }

    public void insertDst(Variable variable) {
        this.dsts.add(variable);
    }

    public int getLineCount() {
        return lineCount;
    }

    public String getOps() {
        return ops;
    }

    public Type getDerivedType() {
        return derivedType;
    }

    public CFGBlock getFuncCall() {
        return funcCall;
    }

    public void setFuncCall(CFGBlock funcCall) {
        this.funcCall = funcCall;
    }

    public static void insertTerms(int index, List<TACTerm> terms, List<TACTerm> insertTerms) {
        terms.addAll(index, insertTerms);
        int size = insertTerms.size();
        for (int i = index + size; i < terms.size(); i++) {
            TACTerm termAfter = terms.get(i);
            termAfter.setLineCount(i + 1);
            if(termAfter.getSrc() != null && termAfter.getSrc().getName().charAt(0) == '(' && Integer.parseInt(termAfter.getSrc().getName(), 1, termAfter.getSrc().getName().length() - 1, 10) > index) {
                termAfter.setSrc(new Variable("(" + (Integer.parseInt(termAfter.getSrc().getName(), 1, termAfter.getSrc().getName().length() - 1, 10) + size) + ")", 0));
            }
            if (termAfter.getOps().equals("CALL")) {
                List<Variable> dsts = termAfter.getDsts();
                for (int j = 0; j < dsts.size(); j++) {
                    if (dsts.get(j).getName().charAt(0) == '(' && Integer.parseInt(dsts.get(j).getName(), 1, dsts.get(j).getName().length() - 1, 10) > index) {
                        dsts.set(j, new Variable("(" + (Integer.parseInt(dsts.get(j).getName(), 1, dsts.get(j).getName().length() - 1, 10) + size) + ")", 0));
                    }
                }

            } else {
                if(termAfter.getDst() != null && terms.get(i).getDst().getName().charAt(0) == '(' && Integer.parseInt(termAfter.getDst().getName(), 1, termAfter.getDst().getName().length() - 1, 10) > index) {
                    termAfter.setDst(new Variable("(" + (Integer.parseInt(termAfter.getDst().getName(), 1, termAfter.getDst().getName().length() - 1, 10) + size) + ")", 0));
                }
            }
        }
    }

    public void setOutputRegister(int outputRegister) {
        this.outputRegister = outputRegister;
    }

    public int getOutputRegister() {
        return outputRegister + 1;
    }

    public void setSrcRegister(int srcRegister) {
        this.srcRegister = srcRegister;
    }

    public int getSrcRegister() {
        return srcRegister + 1;
    }

    public void setDstRegister(int dstRegister) {
        this.dstRegister = dstRegister;
    }

    public int getDstRegister() {
        return dstRegister + 1;
    }

    public void setDstsRegister(List<Integer> dstsRegister) {
        this.dstsRegister = dstsRegister;
    }

    public List<Integer> getDstsRegister() {
        return dstsRegister;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TACTerm)) return false;
        TACTerm term = (TACTerm) o;
        return getLineCount() == term.getLineCount() && uuid == term.uuid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLineCount(), uuid);
    }

    @Override
    public String toString() {
        StringBuilder output;
        if (ops.equals("CALL")) {
            output = new StringBuilder("<" + lineCount + ">" + (isDeleted() ? "deleted-" : "") + lineCount +
                    (outputRegister >= 0 ? ("[R" + (outputRegister == 99 ? "MEM" : outputRegister + 1) + "]") : "") + ":" + ops + " " + (src == null ? "" : src.toString()));
            for (int i = 0; i < dsts.size(); i++) {
                output.append(" ").append(dsts.get(i));
                if (dstsRegister.size() > 0)
                    output.append(dstsRegister.get(i) >= 0 ? ("[R" + (dstsRegister.get(i) == 99 ? "MEM" : dstsRegister.get(i) + 1) + "]") : "");
            }
        } else if (ops.equals("PHI")) {
            output = new StringBuilder("<" + lineCount + ">" + (isDeleted() ? "deleted-" : "") + lineCount + ":" + ops + " ");
            for (Variable variable : dsts) {
                output.append(" ").append(variable);
            }
        } else {
            output = new StringBuilder("<" + lineCount + ">" + (isDeleted() ? "deleted-" : "") + lineCount +
                    (outputRegister >= 0 ? ("[R" + (outputRegister == 99 ? "MEM" : outputRegister + 1) + "]") : "") + ":" + ops + " " + (src == null ? "" : src.toString()) +
                    (srcRegister >= 0 ? ("[R" + (srcRegister == 99 ? "MEM" : srcRegister + 1) + "]") : "") + " " + (dst == null ? "" : dst.toString()) +
                    (dstRegister >= 0 ? ("[R" + (dstRegister == 99 ? "MEM" : dstRegister + 1) + "]") : ""));
        }
        return output.toString();
    }


}
