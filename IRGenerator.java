import error.ErrorMessage;
import ir.*;
import parser.ASTTreeNode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static parser.EBNFUtil.isNumeric;

public class IRGenerator {

    private final SymbolTable symbolTable;
    private List<CFGBlock> cfgBlocks;
    private List<TACTerm> terms;

    // including all functions roots, e.g., orphan functions
    private List<Integer> roots;
    private List<Integer> exits;
    private List<CFGBlock> rootBlocks;
    private List<CFGBlock> exitBlocks;
    private Map<TACTerm, Set<LiveVariable>> exitVariables;
    private Map<CFGBlock, Set<LiveVariable>> variables;

    private TypeChecker typeChecker;
    private RegisterAllocation register;
    private static Set<String> jumps = new HashSet<>(Arrays.asList("BEQ", "BNE", "BLT", "BGE", "BGT", "BLE", "BRA"));
    private static Set<String> arithmatics = new HashSet<>(Arrays.asList("ADD", "ADDA", "SUB", "MUL", "DIV", "CMP", "LOAD", "READ"));

    private int lineCount = 0;
    private int blocks = 0;

    private final String outputPreFilename;
    private final String outputPostFilename;

    private PrintWriter prewriter;
    private PrintWriter postwriter;

    public IRGenerator(String[] args, boolean print) throws IOException, ErrorMessage {
        String[] filenames = args[0].split("/");
        this.outputPreFilename = "./output/pre-" + filenames[filenames.length - 1] + ".dot";
        this.outputPostFilename = "./output/post-" + filenames[filenames.length - 1] + ".dot";

        Parser parser = new Parser(args[0]);
        this.symbolTable = new SymbolTable();

        this.cfgBlocks = new ArrayList<>();
        this.terms = new ArrayList<>();

        this.roots = new ArrayList<>();
        this.exits = new ArrayList<>();
        this.rootBlocks = new ArrayList<>();
        this.exitBlocks = new ArrayList<>();
        this.typeChecker = new TypeChecker();

        while (parser.Derivation()) ;
        newBlock();
        AST2IR(parser.getRoot());
        // last one is main
        for (Integer root : roots) rootBlocks.add(cfgBlocks.get(root - 1));
        for (Integer exit : exits) exitBlocks.add(cfgBlocks.get(exit - 1));

        constructBlocks();

        // roots might be changed
        detectEmptyBlock();
        SSA ssa;
        Analysis analysis;
        if (isNumeric(args[1])) {
            ssa = new SSA(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks);
            analysis = new Analysis(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks, ssa.getCfgForest(), ssa.getCfgReverseForest());
            while (true) {
                boolean change = false;
                if (analysis.AS()) {
                    change = true;
                }
                if (analysis.CF()) {
                    change = true;
                }
                if (analysis.CommonSubexpressionElimination()) {
                    change = true;
                }
                if (analysis.CopyPropagationAndConstantPropagation()) {
                    change = true;
                }
                if (analysis.RS()) {
                    change = true;
                }
                if (analysis.DeadCodeElimination()) {
                    change = true;
                }
                if (!change)
                    break;

            }
            if (print) {
                prewriter = new PrintWriter(this.outputPreFilename);
                dotGraphPrint(prewriter);
            }
            this.register = new RegisterAllocation(Integer.parseInt(args[1]));
            registerOptimization(analysis);
            if (print) {
                postwriter = new PrintWriter(this.outputPostFilename);
                dotGraphPrint(postwriter);
            }
        } else if (args[1].equals("SSA")) {
            if (print) {
                prewriter = new PrintWriter(this.outputPreFilename);
                dotGraphPrint(prewriter);
            }
            new SSA(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks);
            if (print) {
                postwriter = new PrintWriter(this.outputPostFilename);
                dotGraphPrint(postwriter);
            }
        } else if (args[1].equals("AllOptimizations")) {
            ssa = new SSA(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks);
            analysis = new Analysis(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks, ssa.getCfgForest(), ssa.getCfgReverseForest());
            if (print) {
                prewriter = new PrintWriter(this.outputPreFilename);
                dotGraphPrint(prewriter);
            }
            while (true) {
                boolean change = false;
                if (analysis.AS())
                    change = true;
                if (analysis.CF())
                    change = true;
                if (analysis.CommonSubexpressionElimination())
                    change = true;
                if (analysis.CopyPropagationAndConstantPropagation())
                    change = true;
                if (analysis.RS())
                    change = true;
                if (analysis.DeadCodeElimination())
                    change = true;
                if (!change)
                    break;
            }
            if (print) {
                postwriter = new PrintWriter(this.outputPostFilename);
                dotGraphPrint(postwriter);
            }

        } else if (args[1].equals("OneThrough")){
            ssa = new SSA(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks);
            analysis = new Analysis(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks, ssa.getCfgForest(), ssa.getCfgReverseForest());
            if (print) {
                prewriter = new PrintWriter(this.outputPreFilename);
                dotGraphPrint(prewriter);
            }

            for (int i = 2; i < args.length; i++) {
                switch (args[i]) {
                    case "RS" -> analysis.RS();
                    case "AS" -> analysis.AS();
                    case "CF" -> analysis.CF();
                    case "CSE" -> analysis.CommonSubexpressionElimination();
                    case "CP" -> analysis.CopyPropagationAndConstantPropagation();
                    case "DCE" -> analysis.DeadCodeElimination();
                }
            }
            if (print) {
                postwriter = new PrintWriter(this.outputPostFilename);
                dotGraphPrint(postwriter);
            }

        } else if(args[1].equals("Repeat")) {
            ssa = new SSA(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks);
            analysis = new Analysis(this.symbolTable, this.terms, this.cfgBlocks, this.rootBlocks, this.exitBlocks, ssa.getCfgForest(), ssa.getCfgReverseForest());
            if (print) {
                prewriter = new PrintWriter(this.outputPreFilename);
                dotGraphPrint(prewriter);
            }
            while (true) {
                boolean change = false;
                for (int i = 2; i < args.length; i++) {
                    switch (args[i]) {
                        case "RS" -> {
                            if (analysis.RS())
                                change = true;
                        }
                        case "AS" -> {
                            if (analysis.AS())
                                change = true;
                        }
                        case "CF" -> {
                            if (analysis.CF())
                                change = true;
                        }
                        case "CSE" -> {
                            if (analysis.CommonSubexpressionElimination())
                                change = true;
                        }
                        case "CP" -> {
                            if (analysis.CopyPropagationAndConstantPropagation())
                                change = true;
                        }
                        case "DCE" -> {
                            if (analysis.DeadCodeElimination())
                                change = true;
                        }
                    }
                }
                if (!change)
                    break;
            }
            if (print) {
                postwriter = new PrintWriter(this.outputPostFilename);
                dotGraphPrint(postwriter);
            }
        } else {
            throw new RuntimeException("Unknown Argument");
        }
    }

    private void DFSPrintGraph(CFGBlock block, PrintWriter writer) {
        block.setVisited(true);
        writer.println(block);
        writer.println(block.getConnections());
        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSPrintGraph(cfgBlock, writer);
            }
        }
        for (CFGBlock cfgBlock : block.getFuncCallBlocks()) {
            if (!cfgBlock.isVisited()) {
                DFSPrintGraph(cfgBlock, writer);
            }
        }
    }

    public void dotGraphPrint(PrintWriter writer) throws FileNotFoundException {
        reset();
        writer.println("""
                digraph G {
                graph [rankdir = LR];
                node[shape=record];""");
        DFSPrintGraph(rootBlocks.get(rootBlocks.size() - 1), writer);
        writer.println("}");
        writer.close();
    }


    public List<CFGBlock> getCfgBlocks() {
        return cfgBlocks;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<CFGBlock> getRootBlocks() {
        return rootBlocks;
    }

    public Map<CFGBlock, Set<LiveVariable>> getVariables() {
        return variables;
    }

    private void registerOptimization(Analysis analysis) {
        translatePHI2MOVE();
        analysis.livenessAnalysis();
        this.exitVariables = analysis.getEntriesLiveVariables();
        this.variables = new HashMap<>();
        detectEmptyBlock();
        reset();
        for (CFGBlock block: rootBlocks) {
            Set<LiveVariable> vs = registerAllocation(block);
            this.variables.put(block, vs);
        }
        reset();
    }

    private Set<LiveVariable> registerAllocation(CFGBlock root) {
        reset();
        root.setVisited(true);
        Map<LiveVariable, RegisterGraphNode> nodesMap = new HashMap();
        Queue<CFGBlock> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() != 0) {
            CFGBlock block = queue.poll();
            for (TACTerm term: block.getTerms()) {
                if (term.isDeleted())
                    continue;
                Set<LiveVariable> variables = this.exitVariables.get(term);
                for (LiveVariable variable: variables) {
                    if (nodesMap.get(variable) == null) {
                        nodesMap.put(variable, new RegisterGraphNode(variable));
                    }
                }
                for (LiveVariable variable: variables) {
                    RegisterGraphNode node = nodesMap.get(variable);
                    Set<RegisterGraphNode> nodes = node.getNeighbors();
                    for (LiveVariable liveVariable: variables) {
                        RegisterGraphNode liveNode = nodesMap.get(liveVariable);
                        if (!liveNode.equals(node))
                            nodes.add(liveNode);
                    }
                }
            }
            Set<CFGBlock> blocks = block.getCfgChildren();
            for (CFGBlock cfgBlock : blocks) {
                if (!cfgBlock.isVisited()) {
                    cfgBlock.setVisited(true);
                    queue.add(cfgBlock);
                }
            }
        }

        Set<RegisterGraphNode> valueSet = new HashSet<>(nodesMap.values());
        this.register.ChaitinBrigg(valueSet);
        reset();

        queue.add(root);
        while (queue.size() != 0) {
            CFGBlock block = queue.poll();
            for (TACTerm term: block.getTerms()) {
                if (term.isDeleted())
                    continue;
                if (term.getOps().equals("CALL")) {
                    List<Integer> dstsRegister = new ArrayList<>();
                    for (Variable dst: term.getDsts()) {
                        boolean add = false;
                        for (RegisterGraphNode value: valueSet) {
                            if (value.getVariable().getName().equals(dst.getName()) && Objects.equals(value.getVariable().getIndex(), dst.getIndex())) {
                                dstsRegister.add(value.getColor());
                                add = true;
                                break;
                            }
                        }
                        if (!add)
                            dstsRegister.add(-1);
                    }
                    term.setDstsRegister(dstsRegister);
                    for (RegisterGraphNode value: valueSet) {
                        if (value.getVariable().getName().equals("(" + term.getLineCount() + ")") && value.getVariable().getIndex() == 0) {
                            term.setOutputRegister(value.getColor());
                        }
                    }
                } else {
                    for (RegisterGraphNode value: valueSet) {
                        if (term.getSrc() != null && value.getVariable().getName().equals(term.getSrc().getName()) && Objects.equals(value.getVariable().getIndex(), term.getSrc().getIndex())) {
                            term.setSrcRegister(value.getColor());
                        }
                    }
                    for (RegisterGraphNode value: valueSet) {
                        if (term.getDst() != null && value.getVariable().getName().equals(term.getDst().getName()) && Objects.equals(value.getVariable().getIndex(), term.getDst().getIndex())) {
                            term.setDstRegister(value.getColor());
                        }
                    }

                    if (arithmatics.contains(term.getOps())) {
                        for (RegisterGraphNode value: valueSet) {
                            if (value.getVariable().getName().equals("(" + term.getLineCount() + ")") && value.getVariable().getIndex() == 0) {
                                term.setOutputRegister(value.getColor());
                            }
                        }
                    }
                }

            }
            Set<CFGBlock> blocks = block.getCfgChildren();
            for (CFGBlock cfgBlock : blocks) {
                if (!cfgBlock.isVisited()) {
                    cfgBlock.setVisited(true);
                    queue.add(cfgBlock);
                }
            }
        }
        return nodesMap.keySet();
    }


    private void reset() {
        for (CFGBlock cfgBlock : cfgBlocks) cfgBlock.setVisited(false);
    }

    private void constructBlocks() throws ErrorMessage {
        reset();
        for (CFGBlock root : rootBlocks) {
            DFSConstructBlocks(root);
        }
    }

    private void DFSConstructBlocks(CFGBlock block) throws ErrorMessage {
        block.setVisited(true);
        for (TACTerm term : block.getTerms()) {
            if (term.getOps().equals("CALL")) {
                FunctionSymbol function = symbolTable.lookupFunctionSymbol(term.getSrc().getName(), term.getDsts().size());
                term.setFuncCall(cfgBlocks.get(function.getEntryBlockID() - 1));
            }
        }

        for (int cfgBlockIndex : block.getChildren()) {
            CFGBlock cfgBlock = cfgBlocks.get(cfgBlockIndex);
            block.insertCFGChildren(cfgBlock);
            cfgBlock.insertCFGParent(block);
            if (!cfgBlock.isVisited())
                DFSConstructBlocks(cfgBlock);
        }
    }

    private void detectEmptyBlock() {
        reset();
        for (CFGBlock root : rootBlocks) {
            DFSDetectEmptyBlock(root);
        }
        removeEmptyBlock();
    }

    private void DFSDetectEmptyBlock(CFGBlock block) {
        block.setVisited(true);
        int size = 0;
        for (TACTerm term: block.getTerms())
            if (!term.isDeleted())
                size++;
        if (size == 0 && block.getCfgChildren().size() == 1) {
            block.setDeleted(true);
        }

        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSDetectEmptyBlock(cfgBlock);
            }
        }
    }

    private void removeEmptyBlock() {
        for (CFGBlock block: cfgBlocks) {
            if (block.isDeleted()) {
                if (rootBlocks.contains(block) && block.getCfgChildren().size() == 1) {
                    CFGBlock child = block.getCfgChildren().iterator().next();
                    for (int i = 0; i < rootBlocks.size(); i++) {
                        if (block.equals(rootBlocks.get(i))) {
                            roots.set(i, child.getBlockIndex());
                            rootBlocks.set(i, child);
                            if (block.getCurrentScope() != null) {
                                // function
                                FunctionSymbol functionSymbol = block.getCurrentScope();
                                functionSymbol.setEntryBlockID(child.getBlockIndex());
                                for (TACTerm term: terms) {
                                    if (term.getOps().equals("CALL") && term.getSrc().getName().equals(functionSymbol.getFunctionName())
                                            && term.getDsts().size() == functionSymbol.getParams().size()) {
                                        term.setFuncCall(child);
                                    }
                                }
                            }
                        }
                    }

                } else if (block.getCfgParents().size() > 0 && block.getCfgChildren().size() == 1) {
                    CFGBlock child = block.getCfgChildren().iterator().next();
                    for (CFGBlock cfgBlock : block.getCfgParents()) {
                        for(TACTerm term: cfgBlock.getTerms()) {
                            if ((term.getOps().equals("BEQ") || term.getOps().equals("BNE") || term.getOps().equals("BLT")
                                    || term.getOps().equals("BGE") || term.getOps().equals("BGT") || term.getOps().equals("BLE")) &&
                                Integer.parseInt(term.getDst().getName().substring(1, term.getDst().getName().length() - 1)) == block.getBlockIndex()) {
                                term.setDst(new Variable("[" + child.getBlockIndex() + "]", 0));
                            } else if(term.getOps().equals("BRA") &&
                                    Integer.parseInt(term.getDst().getName().substring(1, term.getDst().getName().length() - 1)) == block.getBlockIndex()) {
                                term.setSrc(new Variable("[" + child.getBlockIndex() + "]", 0));
                            }
                        }
                        cfgBlock.removeCFGChildren(block);
                        cfgBlock.insertCFGChildren(child);
                        cfgBlock.insertLabel(child.getBlockIndex(), cfgBlock.getLabels().get(block.getBlockIndex()));
                        child.removeCFGParent(block);
                        child.insertCFGParent(cfgBlock);
                    }
                }
            }
        }
    }

    private void translatePHI2MOVE() {
        reset();
        for (CFGBlock root : rootBlocks) {
            DFSPHI2Move(root);
        }
    }

    private void DFSPHI2Move(CFGBlock block) {
        block.setVisited(true);
        for (TACTerm term: block.getTerms()) {
            if (term.isDeleted())
                continue;
            if (term.getOps().equals("PHI")) {
                term.setDeleted(true);
                for (int i = 0; i < term.getDsts().size(); i++) {
                    CFGBlock cfgBlock = term.getPhiSources().get(i);
                    int offset = 0;
                    if (jumps.contains(cfgBlock.getTerms().get(cfgBlock.getTerms().size() - 1).getOps())) {
                        offset = 1;
                    }
                    cfgBlock.getTerms().add(cfgBlock.getTerms().size() - offset,
                            new TACTerm(term.getLineCount(), "MOVE", term.getDsts().get(i), new Variable(term.getSrc().getName(), term.getLineCount()), Type.VOID, i));
                }
            }
        }
        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSPHI2Move(cfgBlock);
            }
        }
    }


    private void newBlock() {
        blocks++;
        cfgBlocks.add(new CFGBlock(blocks, symbolTable.getCurrent()));
    }

    private TACTerm generateTAC(String ops, String source, String target) throws ErrorMessage {
        lineCount++;
        Type derivedType;
        TACTerm term;
        switch (ops) {
            case "CALL" -> {
                List<String> params;
                int targetSize;
                if (target != null) {
                    params = Arrays.asList(target.split(","));
                    targetSize = params.size();
                    for (String param : params) {
                        assert (getType(param) == Type.INT) :
                                "Params of function " + source + " with param size " + targetSize + " should be VAR types";
                    }
                } else {
                    params = new ArrayList<>();
                    targetSize = 0;
                }
                derivedType = symbolTable.lookupFunctionSymbol(source, targetSize).getFunctionType();
                term = new TACTerm(lineCount, ops, source, params, derivedType);
            }
            case "RET" -> {
                if (symbolTable.getCurrent() == null) {
                    assert (source == null) : "Should not return anything in main function";
                    derivedType = Type.VOID;
                } else {
                    if (source != null) {
                        assert (symbolTable.getCurrent().getFunctionType() == getType(source)) :
                                "Should not return expression in void function " + symbolTable.getCurrent().getFunctionName();
                        derivedType = Type.INT;
                        symbolTable.getCurrent().insertReturnType(derivedType);
                    } else {
                        assert (symbolTable.getCurrent().getFunctionType() == Type.VOID) :
                                "Return of non-void function " + symbolTable.getCurrent().getFunctionName() + " should be VAR type";
                        derivedType = Type.VOID;
                    }
                }
                term = new TACTerm(lineCount, ops, source, target, derivedType);
            }
            case "MOVE" -> {
                assert getType(source) == getType(target): "MOVE operation should have same type for the two terms";
                assert !isNumeric(target): "MOVE operation cannot have number as second term";
                assert target.charAt(0) != '(': "MOVE operation cannot move to line count variable";
                term = new TACTerm(lineCount, ops, source, target, Type.VOID);
                if (getType(source) == Type.ARRAY)
                    term.setMovingArray(true);
            }
            case "STORE" -> {
                assert (getType(source) == Type.INT) : "STORE should store INT type";
                assert (getType(target) == Type.ARRAY) : "STORE should store into ARRAY type";
                term = new TACTerm(lineCount, ops, source, target, Type.VOID);
            }
            case "ADDA" -> {
                assert (getType(source) == Type.INT) : "ADDA should have INT type offset";
                assert (getType(target) == Type.ARRAY) : "ADDA should match ARRAY type";
                term = new TACTerm(lineCount, ops, source, target, Type.ARRAY);
            }
            case "LOAD" -> {
                assert (getType(source) == Type.ARRAY) : "LOAD should have ARRAY type address";
                term = new TACTerm(lineCount, ops, source, target, Type.INT);
            }
            case "WRITE" -> {
                assert (getType(source) == Type.INT) : "WRITE should have INT type as input";
                term = new TACTerm(lineCount, ops, source, target, Type.VOID);
            }
            default -> {
                derivedType = typeChecker.expression(ops, getType(source), getType(target), lineCount);
                term = new TACTerm(lineCount, ops, source, target, derivedType);
            }
        }

        cfgBlocks.get(blocks - 1).insertTerm(term);
        terms.add(term);
        return term;
    }

    private Type getType(String target) throws ErrorMessage {
        Type dstType;
        if (target == null || target.charAt(0) == '[') {
            return Type.VOID;
        }
        if (target.charAt(0) == '(') {
            dstType = terms.get(Integer.parseInt(target.substring(1, target.length() - 1)) - 1).getDerivedType();
        } else {
            if (isNumeric(target)) {
                dstType = Type.INT;
            } else {
                dstType = symbolTable.lookupType(target);
            }
        }
        return dstType;
    }

    private void generateConnections(int startBlock, int endBlock, String label) {
        cfgBlocks.get(startBlock - 1).insertChildren(endBlock - 1);
        cfgBlocks.get(startBlock - 1).insertLabel(endBlock, label);
    }


    private String AST2IR(ASTTreeNode node) throws ErrorMessage {
        // DFS
        List<ASTTreeNode> children = node.getChildren();
        ASTTreeNode relation;
        String exp1;
        String exp2;

        switch (node.getSymbol()) {
            case "number":
                if (children.size() == 2) {
                    return "-" + children.get(1).getValue();
                } else {
                    return children.get(0).getValue();
                }
            case "ident":
                return children.get(0).getValue();
            case "term":
                if (children.get(1).getChildren().size() > 0) {
                    if (children.get(1).getChildren().get(0).getSymbol().equals("1")) {
                        generateTAC("MUL", AST2IR(children.get(0)), AST2IR(children.get(1)));
                    } else if (children.get(1).getChildren().get(0).getSymbol().equals("2")) {
                        generateTAC("DIV", AST2IR(children.get(0)), AST2IR(children.get(1)));
                    }
                    return "(" + lineCount + ")";
                } else {
                    return AST2IR(children.get(0));
                }
            case "termClosure":
                if (children.get(2).getChildren().size() > 0) {
                    if (children.get(2).getChildren().get(0).getSymbol().equals("3")) {
                        generateTAC("ADD", AST2IR(children.get(1)), AST2IR(children.get(2)));
                    } else if (children.get(2).getChildren().get(0).getSymbol().equals("4")) {
                        generateTAC("SUB", AST2IR(children.get(1)), AST2IR(children.get(2)));
                    }
                    return "(" + lineCount + ")";
                } else {
                    return AST2IR(children.get(1));
                }
            case "factor":
                if (children.size() > 1) {
                    return AST2IR(children.get(1));
                } else {
                    String fac = AST2IR(children.get(0));
                    if (children.get(0).getSymbol().equals("designator") && fac != null && fac.charAt(0) == '(') {
                        generateTAC("LOAD", fac, null);
                        return "(" + lineCount + ")";
                    }
                    return fac;
                }
            case "funcCall":
                ASTTreeNode expressionClosure = children.get(3);
                String funcName;

                if (expressionClosure.getChildren().size() > 0) {
                    List<String> expressions = new ArrayList<>();
                    expressions.add(AST2IR(expressionClosure.getChildren().get(0)));
                    ASTTreeNode expressionSubClosure = expressionClosure.getChildren().get(1);
                    while (expressionSubClosure.getChildren().size() > 0) {
                        expressions.add(AST2IR(expressionSubClosure.getChildren().get(1)));
                        expressionSubClosure = expressionSubClosure.getChildren().get(2);
                    }
                    funcName = AST2IR(children.get(1));
                    assert (funcName != null) : "Calling function should have name";
                    if (!(funcName.equals("OutputNum") || funcName.equals("InputNum") || funcName.equals("OutputNewLine")))
                        generateTAC("CALL", funcName, String.join(",", expressions));
                    else {
                        if (funcName.equals("OutputNum"))
                            for (String expression : expressions)
                                generateTAC("WRITE", expression, null);

                        assert (!funcName.equals("InputNum") && !funcName.equals("OutputNewLine")) : "Calling InputNum/OutputNewLine function should have parameters";

                    }
                } else {
                    funcName = AST2IR(children.get(1));
                    assert (funcName != null) : "Calling function should have name";
                    if (!(funcName.equals("OutputNum") || funcName.equals("InputNum") || funcName.equals("OutputNewLine")))
                        generateTAC("CALL", funcName, null);
                    else {
                        assert (!funcName.equals("OutputNum")) : "Calling OutputNum function should have parameters";
                        if (funcName.equals("InputNum"))
                            generateTAC("READ", null, null);
                        else generateTAC("WRITENL", null, null);

                    }
                }
                return "(" + lineCount + ")";
            case "factorClosure":
                if (children.get(2).getChildren().size() > 0) {
                    if (children.get(2).getChildren().get(0).getSymbol().equals("1")) {
                        generateTAC("MUL", AST2IR(children.get(1)), AST2IR(children.get(2)));
                    } else if (children.get(1).getChildren().get(0).getSymbol().equals("2")) {
                        generateTAC("DIV", AST2IR(children.get(1)), AST2IR(children.get(2)));
                    }
                    return "(" + lineCount + ")";
                } else {
                    return AST2IR(children.get(1));
                }
            case "statement":
                AST2IR(children.get(0));
                if (children.get(0).getSymbol().equals("funcCall")) {
                    TACTerm callTerm = terms.get(terms.size() - 1);
                    assert (callTerm.getDerivedType() == Type.VOID) : "Statement cannot have non-void function " + callTerm.getSrc();
                }
                break;
            case "expression":
                if (children.get(1).getChildren().size() > 0) {
                    if (children.get(1).getChildren().get(0).getSymbol().equals("3")) {
                        generateTAC("ADD", AST2IR(children.get(0)), AST2IR(children.get(1)));
                    } else if (children.get(1).getChildren().get(0).getSymbol().equals("4")) {
                        generateTAC("SUB", AST2IR(children.get(0)), AST2IR(children.get(1)));
                    }
                    return "(" + lineCount + ")";
                } else {
                    return AST2IR(children.get(0));
                }
            case "designator":
                if (children.get(1).getChildren().size() == 0) {
                    return AST2IR(children.get(0));
                } else {
                    String ident = AST2IR(children.get(0));
                    Type type = symbolTable.lookupType(ident);
                    assert (type == Type.ARRAY) : "Indexing VAR type instead of ARRAY type";
                    List<Integer> lengths = symbolTable.lookupArrayParam(ident);
                    List<String> designators = new ArrayList<>();
                    ASTTreeNode designatorClosure = children.get(1);
                    while (designatorClosure.getChildren().size() > 0) {
                        designators.add(AST2IR(designatorClosure.getChildren().get(1)));
                        designatorClosure = designatorClosure.getChildren().get(3);
                    }
                    assert (designators.size() == lengths.size()) : "ARRAY Indexing should use the same length as the declaration";
                    if (designators.size() > 1) {
                        generateTAC("MUL", designators.get(0), String.valueOf(lengths.get(1)));
                        generateTAC("ADD", "(" + lineCount + ")", designators.get(1));
                        for (int i = 1; i < designators.size() - 1; i++) {
                            generateTAC("MUL", "(" + lineCount + ")", String.valueOf(lengths.get(i + 1)));
                            generateTAC("ADD", "(" + lineCount + ")", designators.get(i + 1));
                        }
                        generateTAC("MUL", "(" + lineCount + ")", "4");
                    } else {
                        generateTAC("MUL", designators.get(0), "4");
                    }

                    generateTAC("ADDA", "(" + lineCount + ")", ident);
                    return "(" + lineCount + ")";
                }

            case "assignment":
                String des = AST2IR(children.get(1));
                String res;
                ASTTreeNode sub = children.get(2);

                if (sub.getChildren().get(0).getSymbol().equals("40")) {
                    res = AST2IR(sub.getChildren().get(1));
                } else if (sub.getChildren().get(0).getSymbol().equals("shortHandOp")) {
                    String exp = AST2IR(sub.getChildren().get(1));
                    String ops = sub.getChildren().get(0).getChildren().get(0).getSymbol();
                    switch (ops) {
                        case "11" -> generateTAC("MUL", exp, des);
                        case "12" -> generateTAC("DIV", exp, des);
                        case "13" -> generateTAC("ADD", exp, des);
                        case "14" -> generateTAC("SUB", exp, des);
                        default -> throw new ErrorMessage("IR Generation", "Proper shorthand operation", "Invalid operation");
                    }
                    res = "(" + lineCount + ")";
                } else {
                    String ops = sub.getChildren().get(0).getChildren().get(0).getSymbol();
                    if (ops.equals("51")) {
                        generateTAC("ADD", des, "1");
                    } else {
                        generateTAC("SUB", des, "1");
                    }
                    res = "(" + lineCount + ")";
                }

                if (des != null && des.charAt(0) == '(') {
                    generateTAC("STORE", res, des);
                } else {
                    generateTAC("MOVE", res, des);
                }

                break;
            case "varDecl":
                String type = AST2IR(children.get(0));
                List<String> idents = new ArrayList<>();
                idents.add(AST2IR(children.get(1)));
                ASTTreeNode identClosure = children.get(2);
                while (identClosure.getChildren().size() > 0) {
                    idents.add(AST2IR(identClosure.getChildren().get(1)));
                    identClosure = identClosure.getChildren().get(2);
                }
                for (String s : idents) {
                    if (symbolTable.getCurrent() == null) {
                        // main block
                        if (type == null) {
                            symbolTable.insertGlobal(s, Type.INT);
                        } else {
                            Type arr = Type.ARRAY;
                            symbolTable.insertArrayParams(s, Arrays.stream(type.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
                            symbolTable.insertGlobal(s, arr);
                        }
                    } else {
                        FunctionSymbol current = symbolTable.getCurrent();
                        if (type == null) {
                            current.insertLocal(s, Type.INT);
                        } else {
                            Type arr = Type.ARRAY;
                            current.insertArrayParams(s, Arrays.stream(type.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
                            current.insertLocal(s, arr);
                        }
                    }
                }
                break;
            case "typeDecl":
                if (children.get(0).getSymbol().equals("110")) {
                    return null;
                } else {
                    List<String> numbers = new ArrayList<>();
                    numbers.add(AST2IR(children.get(2)));
                    ASTTreeNode numberClosure = children.get(4);
                    while (numberClosure.getChildren().size() > 0) {
                        numbers.add(AST2IR(numberClosure.getChildren().get(1)));
                        numberClosure = numberClosure.getChildren().get(3);
                    }
                    return String.join(",", numbers);
                }
            case "ifStatement":
                relation = children.get(1);
                exp1 = AST2IR(relation.getChildren().get(0));
                exp2 = AST2IR(relation.getChildren().get(2));
                generateTAC("CMP", exp1, exp2);
                // jump to else block, need to fix
                TACTerm ifTerm = comparison(relation, blocks);
                CFGBlock ifStatement = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                ifStatement.setBlockType(BlockType.IF);
                int startingBlock = blocks;

                // if block
                generateConnections(startingBlock, blocks + 1, "then");
                newBlock();
                ifStatement.thenBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                AST2IR(children.get(3));
                // jump to join block, need to fix
                TACTerm jumpTerm = generateTAC("BRA", "[" + blocks + "]", null);
                int ifBlock = blocks;

                // else block
                generateConnections(startingBlock, blocks + 1, "else");
                newBlock();
                ifStatement.elseBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                ifTerm.setDst(new Variable("[" + blocks + "]", 0));
                AST2IR(children.get(4));
                int elseBlock = blocks;

                generateConnections(ifBlock, blocks + 1, null);
                generateConnections(elseBlock, blocks + 1, null);
                newBlock();
                ifStatement.joinBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                jumpTerm.setSrc(new Variable("[" + blocks + "]", 0));
                break;
            case "whileStatement":
                // comparison block
                if (this.cfgBlocks.get(this.cfgBlocks.size() - 1).getTerms().size() != 0 || this.roots.contains(this.cfgBlocks.get(this.cfgBlocks.size() - 1).getBlockIndex())) {
                    generateConnections(blocks, blocks + 1, null);
                    newBlock();
                }
                relation = children.get(1);
                exp1 = AST2IR(relation.getChildren().get(0));
                exp2 = AST2IR(relation.getChildren().get(2));
                generateTAC("CMP", exp1, exp2);
                // jump to else block, need to fix
                TACTerm whileTerm = comparison(relation, blocks);

                CFGBlock whileStatement = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                whileStatement.setBlockType(BlockType.WHILE);
                int comparisonBlock = blocks;

                generateConnections(blocks, blocks + 1, "then");
                newBlock();
                whileStatement.whileThenBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                AST2IR(children.get(3));
                generateTAC("BRA", "[" + comparisonBlock + "]", null);
                generateConnections(blocks, comparisonBlock, null);

                // comparison block is not more than 1 block, so directly use
                generateConnections(comparisonBlock, blocks + 1, "else");
                newBlock();
                whileStatement.whileElseBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                whileTerm.setDst(new Variable("[" + blocks + "]", 0));
                break;
            case "repeatStatement":
                // repeat body block
                if (this.cfgBlocks.get(this.cfgBlocks.size() - 1).getTerms().size() != 0 || this.roots.contains(this.cfgBlocks.get(this.cfgBlocks.size() - 1).getBlockIndex())) {
                    generateConnections(blocks, blocks + 1, null);
                    newBlock();
                }
                int repeatBlock = blocks;
                CFGBlock repeatStatement = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                AST2IR(children.get(1));

                generateConnections(blocks, blocks + 1, null);
                // comparison block is not more than 1 block, so directly use
                newBlock();
                relation = children.get(3);
                exp1 = AST2IR(relation.getChildren().get(0));
                exp2 = AST2IR(relation.getChildren().get(2));
                generateTAC("CMP", exp1, exp2);
                comparison(relation, repeatBlock);
                repeatStatement.repeatCompareBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);

                generateConnections(blocks, repeatBlock, "then");
                generateConnections(blocks, blocks + 1, "else");
                newBlock();
                repeatStatement.repeatElseBlock = this.cfgBlocks.get(this.cfgBlocks.size() - 1);
                break;
            case "returnStatement":
                if (children.get(1).getChildren().size() > 0) {
                    assert (symbolTable.getCurrent() != null) : "Should not return anything in main function";
                    generateTAC("RET", AST2IR(children.get(1).getChildren().get(0)), null);
                } else {
                    generateTAC("RET", null, null);
                }
                break;
            case "funcDecl":
                String ident;
                Type functionType;
                if (children.get(0).getSymbol().equals("63")) {
                    functionType = Type.VOID;
                    ident = AST2IR(children.get(2));
                } else {
                    functionType = Type.INT;
                    ident = AST2IR(children.get(1));
                }
                String parameters = AST2IR(children.get(children.size() - 3));
                FunctionSymbol scope = new FunctionSymbol(symbolTable, functionType, ident, parameters == null ? new ArrayList<>() : Arrays.asList(parameters.split(",")), blocks);
                symbolTable.insertFunctionSymbol(scope);
                symbolTable.setCurrent(scope);
                roots.add(blocks);
                cfgBlocks.get(blocks - 1).setCurrentScope(scope);
                AST2IR(children.get(children.size() - 2));
                if (functionType == Type.VOID && scope.getReturnTypes().size() > 0) {
                    throw new ErrorMessage("Type Checking", "Empty return", "Should not return value in void function " + ident + "(" + parameters + ")");
                }
                if (functionType == Type.INT && scope.getReturnTypes().size() == 0) {
                    throw new ErrorMessage("Type Checking", "Value return", "Function " + ident + "(" + parameters + ")" + " should return value");
                }
                if (cfgBlocks.get(cfgBlocks.size() - 1).getTerms().size() == 0 || !terms.get(terms.size() - 1).getOps().equals("RET"))
                    generateTAC("RET", null, null);
                exits.add(blocks);
                newBlock();
                break;
            case "formalParam":
                List<String> params = new ArrayList<>();
                ASTTreeNode formalParamStatement = children.get(1);
                if (formalParamStatement.getChildren().size() > 0) {
                    params.add(AST2IR(formalParamStatement.getChildren().get(0)));
                    ASTTreeNode paramIdentClosure = formalParamStatement.getChildren().get(1);
                    while (paramIdentClosure.getChildren().size() > 0) {
                        params.add(AST2IR(paramIdentClosure.getChildren().get(1)));
                        paramIdentClosure = paramIdentClosure.getChildren().get(2);
                    }
                    return String.join(",", params);
                } else {
                    return null;
                }
            case "computation":
                AST2IR(children.get(1));
                AST2IR(children.get(2));
                symbolTable.setCurrent(null);
                roots.add(blocks);
                cfgBlocks.get(blocks - 1).setCurrentScope(null);
                AST2IR(children.get(4));
                if (cfgBlocks.get(cfgBlocks.size() - 1).getTerms().size() == 0 || !cfgBlocks.get(cfgBlocks.size() - 1).getTerms().get(cfgBlocks.get(cfgBlocks.size() - 1).getTerms().size() - 1).getOps().equals("RET"))
                    generateTAC("RET", null, null);
                exits.add(blocks);
                break;
            default:
                for (ASTTreeNode child : children) {
                    AST2IR(child);
                }
        }
        return null;
    }

    private TACTerm comparison(ASTTreeNode relation, int b) throws ErrorMessage {
        switch (relation.getChildren().get(1).getChildren().get(0).getSymbol()) {
            case "20" -> {
                // ==
                return generateTAC("BNE", "(" + lineCount + ")", "[" + b + "]");
            }
            case "21" -> {
                // !=
                return generateTAC("BEQ", "(" + lineCount + ")", "[" + b + "]");
            }
            case "22" -> {
                // <
                return generateTAC("BGE", "(" + lineCount + ")", "[" + b + "]");
            }
            case "23" -> {
                // >=
                return generateTAC("BLT", "(" + lineCount + ")", "[" + b + "]");
            }
            case "24" -> {
                // <=
                return generateTAC("BGT", "(" + lineCount + ")", "[" + b + "]");
            }
            default -> {
                // >
                return generateTAC("BLE", "(" + lineCount + ")", "[" + b + "]");
            }
        }
    }

}
