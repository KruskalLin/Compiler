package ir;

import error.ErrorMessage;

import java.util.*;
import java.util.stream.Collectors;

import static parser.EBNFUtil.isNumeric;

/**
 * @LocalOptim DFS every block
 * @GlobalOptim top-bottom --> BFS dom tree; bottom-top --> BFS reverse dom tree
 * @Boolean to show if there are changes to ensure convergence
 */

public class Analysis {

    private SymbolTable symbolTable;
    private List<TACTerm> terms;
    private List<List<CFGBlock>> cfgForest;
    private List<List<CFGBlock>> cfgReverseForest;
    private List<CFGBlock> cfgBlocks;
    private List<CFGBlock> rootBlocks;
    private List<CFGBlock> exitBlocks;
    private Queue<CFGBlock> queue;
    private Map<TACTerm, Set<LiveVariable>> entriesLiveVariables;
    private Map<TACTerm, Set<LiveVariable>> exitsLiveVariables;


    public Analysis(SymbolTable symbolTable, List<TACTerm> terms, List<CFGBlock> cfgBlocks,
                    List<CFGBlock> rootBlocks, List<CFGBlock> exitBlocks,
                    List<List<CFGBlock>> cfgForest, List<List<CFGBlock>> cfgReverseForest) throws ErrorMessage {
        this.symbolTable = symbolTable;
        this.terms = terms;
        this.cfgBlocks = cfgBlocks;
        this.rootBlocks = rootBlocks;
        this.exitBlocks = exitBlocks;
        this.cfgForest = cfgForest;
        this.cfgReverseForest = cfgReverseForest;
        reset();

    }

    public boolean PHItranslation() {
        reset();
        boolean change = false;
        TranslatePHI phi = new TranslatePHI();
        for (CFGBlock rootBlock: rootBlocks) {
            if (DFSOptimize(rootBlock, phi))
                change = true;
            reset();
        }
        reset();
        return change;
    }

    public boolean RS() {
        reset();
        boolean change = false;
        RemoveRedundantStore rs = new RemoveRedundantStore();
        for (CFGBlock rootBlock: rootBlocks) {
            if (DFSOptimize(rootBlock, rs))
                change = true;
            reset();
        }
        reset();
        return change;
    }

    public boolean AS() {
        reset();
        boolean change = PHItranslation();
        for (CFGBlock rootBlock : rootBlocks) {
            while (DFS(rootBlock, new Simplification())) {
                change = true;
                reset();
            }
        }
        reset();
        return change;
    }

    public boolean CF() {
        reset();
        boolean change = PHItranslation();
        for (CFGBlock rootBlock : rootBlocks) {
            while (DFS(rootBlock, new ConstantFold())) {
                change = true;
                reset();
            }
        }
        reset();
        return change;
    }

    public boolean CopyPropagationAndConstantPropagation() {
        reset();
        boolean change = PHItranslation();
        for (CFGBlock rootBlock : rootBlocks) {
            CP cp = new CP();
            while (BFS(rootBlock, cp)) {
                reset();
            }
            reset();
            if (DFSOptimize(rootBlock, cp))
                change = true;
            reset();
        }
        return change;
    }

    public boolean CommonSubexpressionElimination() {
        reset();
        boolean change = PHItranslation();
        for (CFGBlock rootBlock : rootBlocks) {
            CSE cse = new CSE();
            while (BFS(rootBlock, cse)) {
                reset();
            }
            reset();
            if (DFSOptimize(rootBlock, cse))
                change = true;
            reset();
        }
        return change;
    }

    public boolean DeadCodeElimination() {
        reset();
        boolean change = PHItranslation();
        for (CFGBlock exitBlock : exitBlocks) {
            Liveness liveness = new Liveness();
            while (ReverseBFS(exitBlock, liveness)) {
                reset();
            }
            reset();
            if (ReverseDFSOptimize(exitBlock, liveness))
                change = true;
            reset();
        }
        return change;
    }


    private void reset() {
        for (CFGBlock cfgBlock : cfgBlocks) cfgBlock.setVisited(false);
    }

    private interface GlobalOptimization {
        boolean process(CFGBlock block);
        boolean optimize(CFGBlock block);
    }

    private class AvailableExpressionWrap {
        private final Integer lineCount;
        private final AvailableExpression expression;
        public AvailableExpressionWrap(AvailableExpression expression) {
            this.lineCount = expression.getLineCount();
            this.expression = expression;
        }

        public Integer getLineCount() {
            return lineCount;
        }

        public AvailableExpression getExpression() {
            return expression;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AvailableExpressionWrap)) return false;
            AvailableExpressionWrap that = (AvailableExpressionWrap) o;
            return getLineCount().equals(that.getLineCount()) && getExpression().equals(that.getExpression());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getLineCount(), getExpression());
        }
    }

    private class CSE implements GlobalOptimization {

        private Map<Integer, Set<AvailableExpression>> analysisMap;
        private Map<TACTerm, Set<AvailableExpression>> entries;
        private Map<TACTerm, Set<AvailableExpression>> exits;
        private Queue<TACTerm> availableTerms;

        public CSE() {
            analysisMap = new HashMap<>();
            entries = new HashMap<>();
            exits = new HashMap<>();
            availableTerms = new LinkedList<>();
        }

        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;
            Set<AvailableExpressionWrap> inputVariablesWrap = null;
            for (CFGBlock cfgBlock: block.getCfgParents()) {
                // intersection
                if (inputVariablesWrap == null) {
                    inputVariablesWrap = analysisMap.getOrDefault(cfgBlock.getBlockIndex(), new HashSet<>()).stream().map(AvailableExpressionWrap::new).collect(Collectors.toSet());
                } else {
                    inputVariablesWrap.retainAll(analysisMap.getOrDefault(cfgBlock.getBlockIndex(), new HashSet<>()).stream().map(AvailableExpressionWrap::new).collect(Collectors.toSet()));
                }
            }
            if (inputVariablesWrap == null)
                inputVariablesWrap = new HashSet<>();
            Set<AvailableExpression> inputVariables = inputVariablesWrap.stream().map(AvailableExpressionWrap::getExpression).collect(Collectors.toSet());;

            for (int i = 0; i < block.getTerms().size(); i++) {
                if (!block.getTerms().get(i).isDeleted()) {
                    availableTerms.add(block.getTerms().get(i));
                }
            }
            if (availableTerms.size() == 0) {
                analysisMap.put(block.getBlockIndex(), inputVariables);
                return false;
            }
            if (entries.get(availableTerms.peek()) == null) {
                entries.put(availableTerms.peek(), inputVariables);
                change = true;
            } else {
                if (!entries.get(availableTerms.peek()).equals(inputVariables)) {
                    change = true;
                    entries.put(availableTerms.peek(), inputVariables);
                }
            }

            while (!availableTerms.isEmpty()) {
                TACTerm term = availableTerms.poll();
                assert (entries.get(term) != null): "Error with CSE analysis algorithm";
                Set<AvailableExpression> entryVariables = entries.get(term);
                Set<AvailableExpression> exitVariables = new HashSet<>(entryVariables);
                exits.put(term, exitVariables);
                if (term.getOps().equals("ADD") || term.getOps().equals("ADDA") || term.getOps().equals("SUB")
                        || term.getOps().equals("MUL") || term.getOps().equals("DIV") || term.getOps().equals("CMP")) {
                    AvailableExpression expression = new AvailableExpression(term.getLineCount(), term.getOps(), term.getSrc(), term.getDst());
                    exitVariables.add(expression);
                }
                if (availableTerms.isEmpty()) {
                    analysisMap.put(block.getBlockIndex(), exitVariables);
                    break;
                }
                entries.put(availableTerms.peek(), exitVariables);
            }
            return change;
        }

        @Override
        public boolean optimize(CFGBlock block) {
            boolean change = false;
            for (int i = 0; i < block.getTerms().size(); i++) {
                TACTerm term = block.getTerms().get(i);
                if (term.isDeleted())
                    continue;
                Set<AvailableExpression> entryVariables = entries.get(block.getTerms().get(i));
                switch (term.getOps()) {
                    case "ADD", "ADDA", "SUB", "MUL", "DIV", "CMP" -> {
                        AvailableExpression expression = new AvailableExpression(-1, term.getOps(), term.getSrc(), term.getDst());
                        for (AvailableExpression entryExpression: entryVariables) {
                            if (entryExpression.equals(expression))
                            if (isChange(terms, new Variable("(" + term.getLineCount() + ")", 0),
                                    new Variable("(" + entryExpression.getLineCount() + ")", 0)))
                                change = true;
                        }
                    }
                    default -> {
                    }
                }
            }
            return change;
        }
    }


    private class CP implements GlobalOptimization {

        private Map<Integer, Set<AvailableExpression>> analysisMap;
        private Map<TACTerm, Set<AvailableExpression>> entries;
        private Map<TACTerm, Set<AvailableExpression>> exits;
        private Queue<TACTerm> availableTerms;

        public CP() {
            analysisMap = new HashMap<>();
            entries = new HashMap<>();
            exits = new HashMap<>();
            availableTerms = new LinkedList<>();
        }

        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;
            Set<AvailableExpression> inputVariables = null;
            for (CFGBlock cfgBlock: block.getCfgParents()) {
                // intersection
                if (inputVariables == null) {
                    inputVariables = new HashSet<>(analysisMap.getOrDefault(cfgBlock.getBlockIndex(), new HashSet<>()));
                } else {
                    inputVariables.addAll(analysisMap.getOrDefault(cfgBlock.getBlockIndex(), new HashSet<>()));
                }
            }

            if (inputVariables == null)
                inputVariables = new HashSet<>();

            for (int i = 0; i < block.getTerms().size(); i++) {
                if (!block.getTerms().get(i).isDeleted()) {
                    availableTerms.add(block.getTerms().get(i));
                }
            }

            if (availableTerms.size() == 0) {
                analysisMap.put(block.getBlockIndex(), inputVariables);
                return false;
            }
            if (entries.get(availableTerms.peek()) == null) {
                entries.put(availableTerms.peek(), inputVariables);
                change = true;
            } else {
                if (!entries.get(availableTerms.peek()).equals(inputVariables)) {
                    change = true;
                    entries.put(availableTerms.peek(), inputVariables);

                }
            }
            while (!availableTerms.isEmpty()) {
                TACTerm term = availableTerms.poll();
                assert (entries.get(term) != null): "Error with CP analysis algorithm";
                Set<AvailableExpression> entryVariables = entries.get(term);
                Set<AvailableExpression> exitVariables = new HashSet<>(entryVariables);
                exits.put(term, exitVariables);
                if (term.getOps().equals("MOVE")) {
                    AvailableExpression expression = new AvailableExpression(term.getLineCount(), term.getOps(), term.getSrc(), term.getDst());
                    exitVariables.add(expression);
                }
                if (availableTerms.isEmpty()) {
                    analysisMap.put(block.getBlockIndex(), exitVariables);
                    break;
                }
                entries.put(availableTerms.peek(), exitVariables);
            }

            return change;
        }

        @Override
        public boolean optimize(CFGBlock block) {
            boolean change = false;
            for (int i = 0; i < block.getTerms().size(); i++) {
                TACTerm term = block.getTerms().get(i);
                if (term.isDeleted())
                    continue;
                Set<AvailableExpression> entryVariables = entries.get(block.getTerms().get(i));
                switch (term.getOps()) {
                    case "ADD":
                    case "ADDA":
                    case "SUB":
                    case "MUL":
                    case "DIV":
                    case "CMP":
                    case "STORE":
                        for (AvailableExpression expression: entryVariables) {
                            if (term.getSrc().equals(expression.getDst())) {
                                term.setSrc(expression.getSrc());
                                change = true;
                            }
                            if (term.getDst().equals(expression.getDst())) {
                                term.setDst(expression.getSrc());
                                change = true;
                            }
                        }
                        break;
                    case "SG":
                    case "WRITE":
                    case "MOVE":
                        for (AvailableExpression expression: entryVariables) {
                            if (term.getSrc().equals(expression.getDst())) {
                                term.setSrc(expression.getSrc());
                                change = true;
                            }
                        }
                        break;
                    case "LG":
                        break;
                    case "RET":
                        if (term.getSrc() != null) {
                            for (AvailableExpression expression: entryVariables) {
                                if (term.getSrc().equals(expression.getDst())) {
                                    term.setSrc(expression.getSrc());
                                    change = true;
                                }
                            }
                        }
                        break;
                    case "CALL":
                        for (AvailableExpression expression: entryVariables) {
                            for (int j = 0; j < term.getDsts().size(); j++) {
                                if (term.getDsts().get(j).equals(expression.getDst())) {
                                    term.getDsts().set(j, expression.getSrc());
                                    change = true;
                                }
                            }
                        }
                        break;
                    case "PHI":
                        Set<Integer> removes = new HashSet<>();
                        for (AvailableExpression expression: entryVariables) {
                            for (int j = 0; j < term.getDsts().size(); j++) {
                                if (term.getDsts().get(j).equals(expression.getDst())) {
                                    for (int k = 0; k < term.getDsts().size(); k++) {
                                        if (k == j)
                                            continue;
                                        if (term.getDsts().get(k).equals(expression.getSrc())) {
                                            removes.add(j);
                                        }
                                    }
                                    if (term.getSrc().getName().equals(expression.getSrc().getName()) && term.getLineCount() == expression.getSrc().getIndex())
                                        removes.add(j);
                                    term.getDsts().set(j, expression.getSrc());
                                    change = true;
                                }
                            }
                        }
                        List<Variable> dsts = new ArrayList<>();
                        List<CFGBlock> phiSources = new ArrayList<>();
                        for (int j = 0; j < term.getDsts().size(); j++) {
                            if (!removes.contains(j)) {
                                dsts.add(term.getDsts().get(j));
                                phiSources.add(term.getPhiSources().get(j));
                            }
                        }
                        term.setDsts(dsts);
                        term.setPhiSources(phiSources);
                        break;
                }
            }
            return change;
        }
    }

    private class Liveness implements GlobalOptimization {
        private Map<Integer, Set<LiveVariable>> analysisMap;
        private Map<TACTerm, Set<LiveVariable>> entries;
        private Map<TACTerm, Set<LiveVariable>> exits;
        private Stack<TACTerm> availableTerms;

        public Liveness() {
            analysisMap = new HashMap<>();
            entries = new HashMap<>();
            exits = new HashMap<>();
            availableTerms = new Stack<>();
        }

        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;
            Set<LiveVariable> inputVariables = new HashSet<>();
            for (CFGBlock cfgBlock: block.getCfgChildren()) {
                // union
                inputVariables.addAll(analysisMap.getOrDefault(cfgBlock.getBlockIndex(), new HashSet<>()));
            }

            for (int i = 0; i < block.getTerms().size(); i++) {
                if (!block.getTerms().get(i).isDeleted()) {
                    availableTerms.add(block.getTerms().get(i));
                }
            }
            if (availableTerms.size() == 0) {
                analysisMap.put(block.getBlockIndex(), inputVariables);
                return false;
            }
            if (exits.get(availableTerms.peek()) == null) {
                exits.put(availableTerms.peek(), inputVariables);
                change = true;
            } else {
                if (!exits.get(availableTerms.peek()).equals(inputVariables)) {
                    change = true;
                    exits.put(availableTerms.peek(), inputVariables);
                }
            }

            while (!availableTerms.empty()) {
                TACTerm term = availableTerms.pop();
                assert (exits.get(term) != null): "Error with Liveness analysis algorithm";
                Set<LiveVariable> exitVariables = exits.get(term);
                Set<LiveVariable> entryVariables = new HashSet<>(exitVariables);
                entries.put(term, entryVariables);
                switch (term.getOps()) {
                    case "ADD":
                    case "ADDA":
                    case "SUB":
                    case "MUL":
                    case "DIV":
                    case "CMP":
                        // kill
                        entryVariables.remove(new LiveVariable("(" + term.getLineCount() + ")"));
                        if (!term.getSrc().isNumber()) {
                            entryVariables.add(new LiveVariable(term.getSrc()));
                        }
                        if (!term.getDst().isNumber()) {
                            entryVariables.add(new LiveVariable(term.getDst()));
                        }
                        break;
                    case "BEQ":
                    case "BNE":
                    case "BLT":
                    case "BGE":
                    case "BGT":
                    case "BLE":
                    case "WRITE":
                    case "SG":
                        if (!term.getSrc().isNumber()) {
                            entryVariables.add(new LiveVariable(term.getSrc()));
                        }
                        break;
                    case "STORE":
                        if (!term.getSrc().isNumber()) {
                            entryVariables.add(new LiveVariable(term.getSrc()));
                        }
                        entryVariables.add(new LiveVariable(term.getDst()));
                        break;
                    case "READ":
                        // kill
                        entryVariables.remove(new LiveVariable("(" + term.getLineCount() + ")"));
                        break;
                    case "LOAD":
                        // kill
                        entryVariables.remove(new LiveVariable("(" + term.getLineCount() + ")"));
                        if (!term.getSrc().isNumber()) {
                            entryVariables.add(new LiveVariable(term.getSrc()));
                        }
                        break;
                    case "LG":
                        entryVariables.remove(new LiveVariable(term.getSrc()));
                        break;
                    case "MOVE":
                        // kill
                        entryVariables.remove(new LiveVariable(term.getDst()));
                        if (!term.getSrc().isNumber()) {
                            entryVariables.add(new LiveVariable(term.getSrc()));
                        }
                        break;
                    case "RET":
                        entryVariables.clear();
                        if (term.getSrc() != null) {
                            if (!term.getSrc().isNumber()) {
                                entryVariables.add(new LiveVariable(term.getSrc()));
                            }
                        }
                        break;
                    case "CALL":
                        // We cannot remove call because of global variables
                        for (int j = 0; j < term.getDsts().size(); j++) {
                            if (!term.getDsts().get(j).isNumber()) {
                                entryVariables.add(new LiveVariable(term.getDsts().get(j)));
                            }
                        }
                        break;
                    case "PHI":
                        // kill
                        entryVariables.remove(new LiveVariable(term.getSrc().getName(), term.getLineCount()));
                        for (int j = 0; j < term.getDsts().size(); j++) {
                            if (!term.getDsts().get(j).isNumber()) {
                                entryVariables.add(new LiveVariable(term.getDsts().get(j)));
                            }
                        }
                        break;
                    case "WRITENL":
                        break;
                }
                if (availableTerms.empty()) {
                    analysisMap.put(block.getBlockIndex(), entryVariables);
                    break;
                }
                exits.put(availableTerms.peek(), entryVariables);
            }
            return change;
        }

        @Override
        public boolean optimize(CFGBlock block) {
            boolean change = false;
            for (int i = block.getTerms().size() - 1; i >= 0; i--) {
                TACTerm term = block.getTerms().get(i);
                if (term.isDeleted())
                    continue;
                Set<LiveVariable> exitVariables = exits.get(block.getTerms().get(i));
                assert exitVariables != null: block.getTerms().get(i).getOps();
                switch (term.getOps()) {
                    case "ADD":
                    case "ADDA":
                    case "SUB":
                    case "MUL":
                    case "DIV":
                    case "CMP":
                    case "LOAD":
                    case "READ":
                        if (!exitVariables.contains(new LiveVariable("(" + term.getLineCount() + ")"))) {
                            term.setDeleted(true);
                            change = true;
                        }
                        break;
                    case "BEQ":
                    case "BNE":
                    case "BLT":
                    case "BGE":
                    case "BGT":
                    case "BLE":
                    case "WRITE":
                    case "STORE":
                    case "RET":
                    case "CALL":
                        break;
                    case "LG":
                        if (!exitVariables.contains(new LiveVariable(term.getSrc()))) {
                            term.setDeleted(true);
                            change = true;
                        }
                        break;
                    case "MOVE":
                        if (!exitVariables.contains(new LiveVariable(term.getDst()))) {
                            term.setDeleted(true);
                            change = true;
                        }
                        break;
                    case "PHI":
                        if (!exitVariables.contains(new LiveVariable(term.getSrc().getName(), term.getLineCount()))) {
                            term.setDeleted(true);
                            change = true;
                        }
                        break;
                }
            }
            return change;
        }

        public Map<TACTerm, Set<LiveVariable>> getEntries() {
            return entries;
        }

        public Map<TACTerm, Set<LiveVariable>> getExits() {
            return exits;
        }

    }

    private class TranslatePHI implements GlobalOptimization {


        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;

            for (TACTerm term: block.getTerms()) {
                if (term.isDeleted())
                    continue;
                if (term.getOps().equals("PHI") && term.getDsts().size() == 1) {
                    term.setOps("MOVE");
                    term.setDst(new Variable(term.getSrc().getName(), term.getLineCount()));
                    term.setSrc(term.getDsts().get(0));
                    term.getPhiSources().clear();
                    change = true;
                }
            }
            return change;
        }

        @Override
        public boolean optimize(CFGBlock block) {
            return process(block);
        }
    }


    private class RemoveRedundantStore implements GlobalOptimization {


        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;

            for (TACTerm term: block.getTerms()) {
                if (term.isDeleted())
                    continue;
                if (term.getOps().equals("SG") && term.getSrc().getName().equals(term.getStoreName()) && term.getSrc().getIndex() == 0) {
                    term.setDeleted(true);
                    change = true;
                }
            }
            return change;
        }

        @Override
        public boolean optimize(CFGBlock block) {
            return process(block);
        }
    }

    private boolean BFS(CFGBlock root, GlobalOptimization globalOptimization) {
        boolean change = false;
        root.setVisited(true);
        queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() != 0) {
            CFGBlock block = queue.poll();
            if (globalOptimization.process(block))
                change = true;
            List<CFGBlock> blocks = block.getIdoms();
            for (CFGBlock cfgBlock : blocks) {
                if (!cfgBlock.isVisited()) {
                    cfgBlock.setVisited(true);
                    queue.add(cfgBlock);
                }
            }
        }
        return change;
    }

    private boolean DFSOptimize(CFGBlock block, GlobalOptimization globalOptimization) {
        block.setVisited(true);
        boolean change = false;
        if (globalOptimization.optimize(block))
            change = true;
        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                if(DFSOptimize(cfgBlock, globalOptimization)) {
                    change = true;
                }
            }
        }
        return change;
    }

    private boolean ReverseBFS(CFGBlock root, GlobalOptimization globalOptimization) {
        boolean change = false;
        root.setVisited(true);
        queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() != 0) {
            CFGBlock block = queue.poll();
            if (globalOptimization.process(block))
                change = true;
            List<CFGBlock> blocks = block.getReverseIDoms();
            for (CFGBlock cfgBlock : blocks) {
                if (!cfgBlock.isVisited()) {
                    cfgBlock.setVisited(true);
                    queue.add(cfgBlock);
                }
            }
        }
        return change;
    }

    private boolean ReverseDFSOptimize(CFGBlock block, GlobalOptimization globalOptimization) {
        block.setVisited(true);
        boolean change = false;
        if (globalOptimization.optimize(block))
            change = true;
        for (CFGBlock cfgBlock : block.getCfgParents()) {
            if (!cfgBlock.isVisited()) {
                if (ReverseDFSOptimize(cfgBlock, globalOptimization))
                    change = true;
            }
        }
        return change;
    }


    private interface LocalOptimization {
        boolean process(CFGBlock block);
    }

    private class Simplification implements LocalOptimization {

        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;
            List<TACTerm> localTerms = block.getTerms();
            for (TACTerm term : localTerms) {
                if (term.isDeleted())
                    continue;
                String ops = term.getOps();
                Variable target = new Variable("(" + term.getLineCount() + ")", 0);
                Variable term1 = term.getSrc();
                Variable term2 = term.getDst();

                switch (ops) {
                    case "MUL":
                        if (term1.getName().equals("0") || term2.getName().equals("0")) {
                            if (isChange(terms, target, new Variable("0", 0)))
                                change = true;
                        } else if (term1.getName().equals("1")) {
                            if (isChange(terms, target, term2))
                                change = true;
                        } else if (term2.getName().equals("1")) {
                            if (isChange(terms, target, term1))
                                change = true;
                        } else if (term1.getName().equals("2")) {
                            term.setOps("ADD");
                            term.setSrc(term2);
                            term.setDst(term2);
                            change = true;
                        } else if (term2.getName().equals("2")) {
                            term.setOps("ADD");
                            term.setSrc(term1);
                            term.setDst(term1);
                            change = true;
                        }
                        break;
                    case "DIV":
                        assert (!term2.getName().equals("0")) : "Dividing zero";
                        if (term2.getName().equals("1")) {
                            if (isChange(terms, target, term1))
                                change = true;
                        }
                        break;
                    case "ADD":
                        if (term1.getName().equals("0")) {
                            if (isChange(terms, target, term2))
                                change = true;
                        } else if (term2.getName().equals("0")) {
                            if (isChange(terms, target, term1))
                                change = true;
                        }
                        break;
                    case "SUB":
                        if (term2.getName().equals("0")) {
                            if (isChange(terms, target, term1))
                                change = true;
                        }
                        break;
                }
            }
            return change;
        }
    }

    private class ConstantFold implements LocalOptimization {

        @Override
        public boolean process(CFGBlock block) {
            boolean change = false;
            List<TACTerm> localTerms = block.getTerms();
            for (TACTerm term : localTerms) {
                if (term.isDeleted())
                    continue;
                String ops = term.getOps();
                Variable target = new Variable("(" + term.getLineCount() + ")", 0);
                Variable term1 = term.getSrc();
                Variable term2 = term.getDst();
                if ((ops.equals("MUL") || ops.equals("DIV") || ops.equals("ADD") || ops.equals("SUB") || ops.equals("CMP")) && isNumeric(term1.getName()) && isNumeric(term2.getName())) {
                    int termValue1 = Integer.parseInt(term1.getName());
                    int termValue2 = Integer.parseInt(term2.getName());
                    switch (ops) {
                        case "MUL" -> { if (isChange(terms, target, new Variable(String.valueOf(termValue1 * termValue2), 0))) change = true; }
                        case "DIV" -> {
                            assert (!term2.getName().equals("0")) : "Dividing zero";
                            if (isChange(terms, target, new Variable(String.valueOf(termValue1 / termValue2), 0)))
                                change = true;
                        }
                        case "ADD" -> { if (isChange(terms, target, new Variable(String.valueOf(termValue1 + termValue2), 0))) change = true; }
                        case "SUB" -> { if (isChange(terms, target, new Variable(String.valueOf(termValue1 - termValue2), 0))) change = true; }
                        case "CMP" -> { if (isChange(terms, target, new Variable(String.valueOf((int) Math.signum(termValue1 - termValue2)), 0))) change = true; }
                    }
                }
            }
            return change;
        }
    }

    private boolean isChange(List<TACTerm> terms, Variable source, Variable target) {
        boolean change = false;
        for (TACTerm termChange: terms) {
            if (termChange.isDeleted())
                continue;
            if (termChange.getOps().equals("PHI")) {
                Set<Integer> removes = new HashSet<>();
                for (int i = 0; i < termChange.getDsts().size(); i++) {
                    if (termChange.getDsts().get(i).equals(source)) {
                        for (int j = 0; j < termChange.getDsts().size(); j++) {
                            if (j == i)
                                continue;
                            if (termChange.getDsts().get(j).equals(target))
                                removes.add(i);
                        }
                        if (termChange.getSrc().getName().equals(target.getName()) && termChange.getLineCount() == target.getIndex())
                            removes.add(i);

                        termChange.getDsts().set(i, target);
                        change = true;
                    }
                }
                List<Variable> dsts = new ArrayList<>();
                List<CFGBlock> phiSources = new ArrayList<>();
                for (int i = 0; i < termChange.getDsts().size(); i++) {
                    if (!removes.contains(i)) {
                        dsts.add(termChange.getDsts().get(i));
                        phiSources.add(termChange.getPhiSources().get(i));
                    }
                }
                termChange.setDsts(dsts);
                termChange.setPhiSources(phiSources);

            } else if (termChange.getOps().equals("CALL")) {
                List<Variable> dsts = termChange.getDsts();
                for (int i = 0; i < dsts.size(); i++) {
                    if (dsts.get(i).equals(source)) {
                        dsts.set(i, target);
                        change = true;
                    }
                }
            } else {
                if (termChange.getSrc() != null && termChange.getSrc().equals(source)) {
                    termChange.setSrc(target);
                    change = true;
                }

                if (termChange.getDst() != null && termChange.getDst().equals(source)) {
                    termChange.setDst(target);
                    change = true;
                }
            }
        }
        return change;
    }


    private boolean DFS(CFGBlock block, LocalOptimization localOptimization) {
        block.setVisited(true);
        boolean result = false;
        if (localOptimization.process(block))
            result = true;
        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                if (DFS(cfgBlock, localOptimization))
                    result = true;
            }
        }
        return result;
    }


    public void livenessAnalysis() {
        reset();
        Liveness liveness = new Liveness();;
        for (CFGBlock exitBlock: exitBlocks) {
            while(ReverseBFS(exitBlock, liveness)) {
                reset();
            }
            reset();
        }
        this.entriesLiveVariables = liveness.getEntries();
        this.exitsLiveVariables = liveness.getExits();
    }

    public Map<TACTerm, Set<LiveVariable>> getEntriesLiveVariables() {
        return entriesLiveVariables;
    }

}
