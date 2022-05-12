import error.ErrorMessage;
import ir.*;

import java.io.IOException;
import java.util.*;

public class Compiler {
    private IRGenerator irGenerator;
    private List<CFGBlock> rootBlocks;
    private List<CFGBlock> cfgBlocks;
    private SymbolTable symbolTable;

    private class MachineCode {
        private int lineCount;
        private int ops;
        private Integer a = null;
        private Integer b = null;
        private Integer c = null;

        public MachineCode(int lineCount, int ops, Integer a, Integer b, Integer c) {
            this.lineCount = lineCount;
            this.ops = ops;
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public MachineCode(int lineCount, int ops, Integer a, Integer b) {
            this.lineCount = lineCount;
            this.ops = ops;
            this.a = a;
            this.b = b;
        }

        public MachineCode(int lineCount, int ops, Integer a) {
            this.lineCount = lineCount;
            this.ops = ops;
            this.a = a;
        }

        public MachineCode(int lineCount, int ops) {
            this.lineCount = lineCount;
            this.ops = ops;
        }

        public int getDLX() {
            if (c != null)
                return DLX.assemble(ops, a, b, c);
            else if (b != null)
                return DLX.assemble(ops, a, b);
            else if (a != null)
                return DLX.assemble(ops, a);
            else
                return DLX.assemble(ops);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MachineCode)) return false;
            MachineCode that = (MachineCode) o;
            return lineCount == that.lineCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lineCount);
        }

        @Override
        public String toString() {
            return "lineCount=" + lineCount +
                    ", ops=" + DLX.mnemo[ops] +
                    ", a=" + a +
                    ", b=" + b +
                    ", c=" + c +
                    "\n";
        }
    }

    private class MachineVariable {
        private Variable variable;
        private FunctionSymbol scope;
        private Type type;

        public MachineVariable(Variable variable, FunctionSymbol scope) {
            this.variable = variable;
            this.scope = scope;
            this.type = Type.INT;
            if (isGlobal())
                this.scope = null;
        }

        public MachineVariable(Variable variable, FunctionSymbol scope, Type type) {
            this.variable = variable;
            this.scope = scope;
            this.type = type;
            if (isGlobal())
                this.scope = null;
        }

        public Type getType() {
            return type;
        }

        public FunctionSymbol getScope() {
            return scope;
        }

        public Variable getVariable() {
            return variable;
        }

        public boolean isGlobal() {
            if (variable.getName().charAt(0) == '(')
                return false;
            if (scope == null)
                return true;
            if (scope.getRealGlobalVariables().contains(variable.getName()))
                return true;
            return false;
        }

        public boolean isParam() {
            if (variable.getName().charAt(0) == '(')
                return false;
            if (scope == null)
                return false;
            if (scope.getRealParams().contains(variable.getName()))
                return true;
            return false;
        }

        public boolean isVariable() {
            if (variable.getName().charAt(0) == '(')
                return false;
            if (scope == null)
                return false;
            return !isGlobal() && !isParam();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MachineVariable)) return false;
            MachineVariable that = (MachineVariable) o;
            return Objects.equals(getVariable(), that.getVariable()) && Objects.equals(getScope(), that.getScope()) && getType() == that.getType();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getVariable(), getScope(), getType());
        }

        @Override
        public String toString() {
            return "MachineVariable{" +
                    "variable=" + variable +
                    '}';
        }
    }

    // generate branch to iteself PC first, then fix it
    private Map<MachineCode, TACTerm> branch2Fix = new HashMap<>();
    private Map<TACTerm, Integer> term2PC = new HashMap<>();


    public static int PC = 0;

    // TEMP for globals, variables spilling, parameter passing
    public static int tempIndex = 0;
    private Map<MachineVariable, Integer> globalAddressMap = new HashMap<>();
    public static final Integer GLOBAL_OFFSET = 1999;

    // the integer means offset to FP
    private Map<MachineVariable, Integer> localAddressMap = new HashMap<>();
    private Map<FunctionSymbol, Integer> functionAddressIndex = new HashMap<>();

    public static final Integer R0 = 0;
    public static final Integer RA = 31; // Return Address Pointer
    public static final Integer GLOBAL = 30; // MemSize - 1
    public static final Integer SP = 29; // Stack Pointer
    public static final Integer FP = 28; // Frame Pointer
    public static final Integer TEMP = 27; // Temporary Constant Register
    public static final Integer TEMPA = 26; // Temporary Constant Register
    public static final Integer TEMPB = 25; // Temporary Constant Register
    public static final Integer TEMPC = 24; // Temporary Constant Register

    private List<MachineCode> programs = new ArrayList<>();

    private final int registerNumber;

    private Map<CFGBlock, Set<LiveVariable>> vs;

    private boolean showProgram = false;

    public Compiler(String[] args) throws IOException, ErrorMessage {
        List<String> argsForIR = new ArrayList<>(Arrays.asList(args));
        argsForIR.remove(1);
        registerNumber = Integer.parseInt(argsForIR.get(1));
        if (argsForIR.size() > 2 && argsForIR.get(2).equals("ShowProgram")) {
            showProgram = true;
        }
        irGenerator = new IRGenerator(argsForIR.toArray(String[]::new), false);
        rootBlocks = irGenerator.getRootBlocks();
        cfgBlocks = irGenerator.getCfgBlocks();
        symbolTable = irGenerator.getSymbolTable();
        vs = irGenerator.getVariables();
    }


    private int newAddressIndex(int size) {
        int baseAddress = -GLOBAL_OFFSET + 4 * tempIndex;
        tempIndex += size;
        return baseAddress;
    }


    private int handleSpillInt(MachineVariable v, int reg, int temp, FunctionSymbol functionSymbol) throws ErrorMessage {
        if (reg == 0) {
            programs.add(new MachineCode(PC++, DLX.ADDI, temp, R0, Integer.parseInt(v.getVariable().getName())));
            return temp;
        }

        boolean spill = false;
        if (reg == 100) {
            spill = true;
            reg = temp;
        }
        if (v.isGlobal()) {
            if (v.getVariable().getIndex() == 0 || spill) {
                v = new MachineVariable(new Variable(v.getVariable().getName(), 0), null);
                programs.add(new MachineCode(PC++, DLX.LDW, reg, GLOBAL, globalAddressMap.get(v)));
            }
        } else {
            if ((v.getVariable().getIndex() == 0 && v.getVariable().getName().charAt(0) != '(') || spill) {
                programs.add(new MachineCode(PC++, DLX.LDW, reg, FP, localAddressMap.get(v) * 4));
            }
        }
        return reg;
    }

    private void arithmatic(TACTerm term, FunctionSymbol scope) throws ErrorMessage {
        int a = term.getOutputRegister();
        int b = term.getSrcRegister();
        int c = term.getDstRegister();
        MachineVariable va = new MachineVariable(new Variable("(" + term.getLineCount() + ")", 0), scope);
        MachineVariable vb = new MachineVariable(term.getSrc(), scope);
        MachineVariable vc = new MachineVariable(term.getDst(), scope);

        boolean spilla = a == 100;

        a = handleSpillInt(va, a, TEMPA, scope);
        b = handleSpillInt(vb, b, TEMPB, scope);
        c = handleSpillInt(vc, c, TEMPC, scope);

        switch (term.getOps()) {
            case "ADD" -> programs.add(new MachineCode(PC++, DLX.ADD, a, b, c));
            case "MUL" -> programs.add(new MachineCode(PC++, DLX.MUL, a, b, c));
            case "SUB" -> programs.add(new MachineCode(PC++, DLX.SUB, a, b, c));
            case "DIV" -> programs.add(new MachineCode(PC++, DLX.DIV, a, b, c));
            case "CMP" -> programs.add(new MachineCode(PC++, DLX.CMP, a, b, c));
        }

        if (spilla) {
            programs.add(new MachineCode(PC++, DLX.STW, a, FP, localAddressMap.get(va) * 4));
        }

    }

    private void conditionalBranch(TACTerm term, FunctionSymbol scope) throws ErrorMessage {
        term.nextTerm = nextAvailableTerm(cfgBlocks.get(Integer.parseInt(term.getDst().getName().substring(1, term.getDst().getName().length() - 1)) - 1));
        int b = term.getSrcRegister();
        MachineVariable vb = new MachineVariable(term.getSrc(), scope);

        b = handleSpillInt(vb, b, TEMPB, scope);

        MachineCode code = switch (term.getOps()) {
            case "BNE" -> new MachineCode(PC++, DLX.BNE, b, PC);
            case "BEQ" -> new MachineCode(PC++, DLX.BEQ, b, PC);
            case "BGE" -> new MachineCode(PC++, DLX.BGE, b, PC);
            case "BLT" -> new MachineCode(PC++, DLX.BLT, b, PC);
            case "BGT" -> new MachineCode(PC++, DLX.BGT, b, PC);
            case "BLE" -> new MachineCode(PC++, DLX.BLE, b, PC);
            default -> null;
        };
        programs.add(code);
        branch2Fix.put(code, term);
    }

    private void funcCall(TACTerm term, FunctionSymbol preFunctionSymbol) throws ErrorMessage {
        term.nextTerm = nextAvailableTerm(term.getFuncCall());
        // only push unshadow parameters
        FunctionSymbol functionSymbol = symbolTable.lookupFunctionSymbol(term.getSrc().getName(), term.getDsts().size());

        // push locals
        for (int reg = 1; reg <= this.registerNumber; reg++) {
            programs.add(new MachineCode(PC++, DLX.PSH, reg, SP, 4));
        }


        List<Variable> dsts = term.getDsts();
        List<Variable> realDsts = new ArrayList<>();
        List<Integer> registers = term.getDstsRegister();
        List<Integer> realRegisters = new ArrayList<>();
        for (int i = 0; i < dsts.size(); i++) {
            if (functionSymbol.getParamRealIndices().get(i)) {
                realDsts.add(dsts.get(i));
                realRegisters.add(registers.get(i));
            }
        }

        // push parameters
        for (int i = 0; i < realDsts.size(); i++) {
            int reg = realRegisters.get(i) + 1;
            MachineVariable v = new MachineVariable(realDsts.get(i), preFunctionSymbol);
            reg = handleSpillInt(v, reg, TEMPC, preFunctionSymbol);
            programs.add(new MachineCode(PC++, DLX.PSH, reg, SP, 4));
        }

        // prologue
        programs.add(new MachineCode(PC++, DLX.ADDI, RA, R0, (PC + 4) * 4));
        programs.add(new MachineCode(PC++, DLX.PSH, RA, SP, 4));
        programs.add(new MachineCode(PC++, DLX.PSH, FP, SP, 4));
        programs.add(new MachineCode(PC++, DLX.ADDI, FP, SP, 0));

        MachineCode code = new MachineCode(PC++, DLX.JSR, PC);
        branch2Fix.put(code, term);
        programs.add(code);

        if(functionSymbol.getFunctionType() == Type.INT) {
            programs.add(new MachineCode(PC++, DLX.POP, TEMP, SP, -4));
        }
        // pop locals
        for (int reg = this.registerNumber; reg >= 1; reg--) {
            programs.add(new MachineCode(PC++, DLX.POP, reg, SP, -4));
        }

        if(functionSymbol.getFunctionType() == Type.INT) {
            int reg = term.getOutputRegister();
            MachineVariable variable = new MachineVariable(new Variable("(" + term.getLineCount() + ")", 0), preFunctionSymbol);
            boolean spilla = reg == 100;
            reg = handleSpillInt(variable, reg, TEMPA, preFunctionSymbol);
            programs.add(new MachineCode(PC++, DLX.ADDI, reg, TEMP, 0));
            if (spilla) {
                programs.add(new MachineCode(PC++, DLX.STW, reg, FP, localAddressMap.get(variable) * 4));
            }
        }
    }

    private void funcReturn(TACTerm term, FunctionSymbol functionSymbol) throws ErrorMessage {
        int b = -1;
        if(functionSymbol.getFunctionType() == Type.INT) {
            b = term.getSrcRegister();
            MachineVariable vb = new MachineVariable(term.getSrc(), functionSymbol);
            b = handleSpillInt(vb, b, TEMPB, functionSymbol);
        }

        // pop temps
        for (int i = 1; i < functionAddressIndex.get(functionSymbol); i++)
            programs.add(new MachineCode(PC++, DLX.POP, TEMP, SP, -4));

        // epilogue
        programs.add(new MachineCode(PC++, DLX.ADD, SP, R0, FP));
        programs.add(new MachineCode(PC++, DLX.POP, FP, SP, -4));
        programs.add(new MachineCode(PC++, DLX.POP, RA, SP, -4));

        // pop parameters
        for (int param = functionSymbol.getRealParams().size() - 1; param >= 0; param--) {
            programs.add(new MachineCode(PC++, DLX.POP, TEMPC, SP, -4));
        }

        if (functionSymbol.getFunctionType() == Type.INT) {
            programs.add(new MachineCode(PC++, DLX.PSH, b, SP, 4));
        }

        programs.add(new MachineCode(PC++, DLX.RET, RA));
    }


    private void generateInstruction(TACTerm term, FunctionSymbol functionSymbol) throws ErrorMessage {
        int a, b, c;
        MachineVariable va, vb, vc;
        MachineCode code = null;
        boolean spilla, spillb, spillc;
        switch (term.getOps()) {
            case "ADDA":
                a = term.getOutputRegister();
                b = term.getSrcRegister();
                va = new MachineVariable(new Variable("(" + term.getLineCount() + ")", 0), functionSymbol);
                vb = new MachineVariable(term.getSrc(), functionSymbol);
                vc = new MachineVariable(new Variable(term.getDst().getName(), 0), functionSymbol, Type.ARRAY);
                spilla = a == 100;

                a = handleSpillInt(va, a, TEMPA, functionSymbol);
                b = handleSpillInt(vb, b, TEMPB, functionSymbol);

                // hack
                if (vc.isGlobal()) {
                    programs.add(new MachineCode(PC++, DLX.ADDI, TEMPC, GLOBAL, globalAddressMap.get(vc)));
                } else {
                    programs.add(new MachineCode(PC++, DLX.ADDI, TEMPC, FP, localAddressMap.get(vc) * 4));
                }
                programs.add(new MachineCode(PC++, DLX.ADD, a, b, TEMPC));

                if (spilla) {
                    programs.add(new MachineCode(PC++, DLX.STW, a, FP, localAddressMap.get(va) * 4));
                }
                break;
            case "ADD":
            case "SUB":
            case "MUL":
            case "DIV":
            case "CMP":
                arithmatic(term, functionSymbol);
                break;
            case "BEQ":
            case "BNE":
            case "BLT":
            case "BGE":
            case "BLE":
            case "BGT":
                conditionalBranch(term, functionSymbol);
                break;
            case "BRA":
                term.nextTerm = nextAvailableTerm(cfgBlocks.get(Integer.parseInt(term.getSrc().getName().substring(1, term.getSrc().getName().length() - 1)) - 1));
                code = new MachineCode(PC++, DLX.BSR, PC);
                programs.add(code);
                branch2Fix.put(code, term);
                break;
            case "CALL":
                funcCall(term, functionSymbol);
                break;
            case "MOVE":
                if (term.isMovingArray()) {
                    // TODO
                } else {
                    b = term.getSrcRegister();
                    c = term.getDstRegister();
                    vb = new MachineVariable(term.getSrc(), functionSymbol);
                    vc = new MachineVariable(term.getDst(), functionSymbol);

                    spillc = c == 100;

                    b = handleSpillInt(vb, b, TEMPB, functionSymbol);
                    c = handleSpillInt(vc, c, TEMPC, functionSymbol);

                    programs.add(new MachineCode(PC++, DLX.ADD, c, R0, b));

                    if (spillc) {
                        if (vc.isGlobal()) {
                            vc = new MachineVariable(new Variable(term.getDst().getName(), 0), functionSymbol);
                            programs.add(new MachineCode(PC++, DLX.STW, c, GLOBAL, globalAddressMap.get(vc)));
                        } else {
                            programs.add(new MachineCode(PC++, DLX.STW, c, FP, localAddressMap.get(vc) * 4));
                        }
                    }
                }
                break;
            case "RET":
                if (functionSymbol == null) {
                    // main return is end
                    programs.add(new MachineCode(PC++, DLX.RET, 0));
                } else {
                    funcReturn(term, functionSymbol);
                }
                break;
            case "LOAD":
                a = term.getOutputRegister();
                b = term.getSrcRegister();
                va = new MachineVariable(new Variable("(" + term.getLineCount() + ")", 0), functionSymbol);
                vb = new MachineVariable(term.getSrc(), functionSymbol);
                spilla = a == 100;
                a = handleSpillInt(va, a, TEMPA, functionSymbol);
                b = handleSpillInt(vb, b, TEMPB, functionSymbol);

                programs.add(new MachineCode(PC++, DLX.LDW, a, b, 0));
                if (spilla) {
                    programs.add(new MachineCode(PC++, DLX.STW, a, FP, localAddressMap.get(va) * 4));
                }
                break;
//            case "LG":
//                b = term.getSrcRegister();
//                vb = new MachineVariable(new Variable(term.getSrc().getName(), 0), null);
//                b = handleSpillInt(vb, b, TEMPB, functionSymbol);
//                programs.add(new MachineCode(PC++, DLX.LDW, b, GLOBAL, globalAddressMap.get(vb)));
//                break;
            case "STORE":
                b = term.getSrcRegister();
                c = term.getDstRegister();
                vb = new MachineVariable(term.getSrc(), functionSymbol);
                vc = new MachineVariable(term.getDst(), functionSymbol);

                b = handleSpillInt(vb, b, TEMPB, functionSymbol);
                c = handleSpillInt(vc, c, TEMPC, functionSymbol);

                programs.add(new MachineCode(PC++, DLX.STW, b, c, 0));
                break;
            case "SG":
                b = term.getSrcRegister();
                spillb = b == 100;
                if (symbolTable.lookupType(term.getStoreName()) == Type.ARRAY) {
                    // TODO
                    break;
                }
                vb = new MachineVariable(new Variable(term.getStoreName(), 0), null);
                if (!spillb) {
                    if (b == 0) {
                        programs.add(new MachineCode(PC++, DLX.ADDI, TEMPB, R0, Integer.parseInt(term.getSrc().getName())));
                        b = TEMPB;
                    }
                    programs.add(new MachineCode(PC++, DLX.STW, b, GLOBAL, globalAddressMap.get(vb)));
                }
                break;
            case "READ":
                a = term.getOutputRegister();
                va = new MachineVariable(new Variable("(" + term.getLineCount() + ")", 0), functionSymbol);
                spilla = a == 100;
                a = handleSpillInt(va, a, TEMPA, functionSymbol);

                programs.add(new MachineCode(PC++, DLX.RDI, a));
                if (spilla) {
                    programs.add(new MachineCode(PC++, DLX.STW, a, FP, localAddressMap.get(va) * 4));
                }
                break;
            case "WRITE":
                b = term.getSrcRegister();
                vb = new MachineVariable(term.getSrc(), functionSymbol);
                b = handleSpillInt(vb, b, TEMPB, functionSymbol);
                programs.add(new MachineCode(PC++, DLX.WRD, b));
                break;
            case "WRITENL":
                programs.add(new MachineCode(PC++, DLX.WRL));
                break;
            default:
                throw new RuntimeException("Unregconized Operation");
        }

    }

    private void generate(CFGBlock root) throws ErrorMessage {
        // Reset frame pointer
        programs.add(new MachineCode(PC++, DLX.ADDI, FP, SP, 0));

        int offset = 1;
        // push temps array
        if (root.getCurrentScope() != null) {
            FunctionSymbol functionSymbol = root.getCurrentScope();
            for (LiveVariable v : vs.get(root)) {
                int index = functionAddressIndex.get(functionSymbol);
                if (functionSymbol.lookupType(v.getName()) == Type.ARRAY) {
                    MachineVariable machineVariable = new MachineVariable(new Variable(v.getName(), 0), functionSymbol, Type.ARRAY);
                    List<Integer> arrayParams = functionSymbol.lookupArrayParams(v.getName());
                    int size = 1;
                    for (int i = 0; i < arrayParams.size(); i++) {
                        size *= arrayParams.get(i);
                    }
                    for (int i = 0; i < size; i++) {
                        programs.add(new MachineCode(PC++, DLX.PSH, R0, SP, 4));
                        offset++;
                    }
                    localAddressMap.put(machineVariable, index);
                    functionAddressIndex.put(functionSymbol, index + size);
                } else {
                    MachineVariable machineVariable = new MachineVariable(new Variable(v.getName(), v.getIndex()), functionSymbol);
                    if (machineVariable.isVariable() || v.getName().charAt(0) == '(') {
                        programs.add(new MachineCode(PC++, DLX.PSH, R0, SP, 4));
                        offset++;
                        localAddressMap.put(machineVariable, index);
                        functionAddressIndex.put(functionSymbol, index + 1);
                    }
                }
            }

            List<String> params = functionSymbol.getRealParams();
            for (int i = 0; i < params.size(); i++) {
                MachineVariable machineVariable = new MachineVariable(new Variable(params.get(i), 0), functionSymbol);
                Integer paramLoc = i - params.size() - 1;
                localAddressMap.put(machineVariable, paramLoc);
            }
        } else {
            for (LiveVariable v : vs.get(root)) {
                int index = functionAddressIndex.get(new FunctionSymbol(-1));
                MachineVariable machineVariable = new MachineVariable(new Variable(v.getName(), v.getIndex()), null);
                if (v.getName().charAt(0) == '(') {
                    programs.add(new MachineCode(PC++, DLX.PSH, R0, SP, 4));
                    offset++;
                    localAddressMap.put(machineVariable, index);
                    functionAddressIndex.put(new FunctionSymbol(-1), index + 1);
                }
            }
        }

        CFGBlock block = root;
        nextAvailableTerm(root).offset = offset;
        while (block != null) {
            block = generateBlock(block);
        }
    }

    private void generateTerms(CFGBlock block) throws ErrorMessage {
        for (TACTerm term: block.getTerms()) {
            if (term.isDeleted())
                continue;
            term2PC.put(term, PC);
            generateInstruction(term, block.getCurrentScope());
        }
    }

    private TACTerm nextAvailableTerm(CFGBlock block) {
        while (block.isDeleted())
            block = block.getCfgChildren().iterator().next();

        for (TACTerm term: block.getTerms()) {
            if (term.isDeleted())
                continue;
            return term;
        }
        throw new RuntimeException("Non-existed target instruction for branch");
    }

    private CFGBlock generateBlock(CFGBlock block) throws ErrorMessage {
        if (block.isDeleted())
            return generateBlock(block.getCfgChildren().iterator().next());

        if (block.repeatCompareBlock != null) {
            // repeat first
            CFGBlock compareBlock = block.repeatCompareBlock;
            CFGBlock elseBlock = block.repeatElseBlock;
            block = generateSubBlock(block);
            while(!block.equals(compareBlock)) {
                block = generateBlock(block);
                if(block == null) {
                    return null;
                }
            }
            generateSubBlock(compareBlock);
            return generateBlock(elseBlock);
        } else {
            return generateSubBlock(block);
        }
    }

    private CFGBlock generateSubBlock(CFGBlock block) throws ErrorMessage {
        if (block.isDeleted())
            generateBlock(block.getCfgChildren().iterator().next());

        if (block.getBlockType() == BlockType.IF) {
            generateTerms(block);

            CFGBlock tBlock = block.thenBlock;
            while(!(tBlock.equals(block.joinBlock))) {
                tBlock = generateBlock(tBlock);
                if(tBlock == null) {
                    return null;
                }
            }

            CFGBlock eBlock = block.elseBlock;
            if (!eBlock.isDeleted()) {
                while (!(eBlock.equals(block.joinBlock))) {
                    eBlock = generateBlock(eBlock);
                    if (eBlock == null)
                        return null;
                }
            }

            return generateBlock(block.joinBlock);

        } else if (block.getBlockType() == BlockType.WHILE){
            generateTerms(block);
            CFGBlock loopBlock = block.whileThenBlock;
            while(!loopBlock.equals(block)) {
                loopBlock = generateBlock(loopBlock);
                if(loopBlock == null) {
                    return null;
                }
            }
            return generateBlock(block.whileElseBlock);

        } else {
            generateTerms(block);
            if (block.getCfgChildren().size() == 0)
                return null;
            return block.getCfgChildren().iterator().next();
        }

    }

    private void insertRealPC() {
        for (MachineCode code : branch2Fix.keySet()) {
            if (code.ops == DLX.JSR) {
                // offset push temps and addi
                code.a = term2PC.get(branch2Fix.get(code).nextTerm) * 4 - 4 * branch2Fix.get(code).nextTerm.offset;
            } else {
                if (code.ops == DLX.BSR)
                    code.a = term2PC.get(branch2Fix.get(code).nextTerm) - code.lineCount;
                else
                    code.b = term2PC.get(branch2Fix.get(code).nextTerm) - code.lineCount;

            }
        }
    }


    // notice: the first function is main. This is a pain if we don't have function addresses. And also
    // the stack should start after the program!
    // we should fix it after main is done
    public int[] getProgram() throws ErrorMessage {
        // fix it later
        programs.add(new MachineCode(PC++, DLX.ADDI, SP, 0, 0));

        // initialize globals
        for (String global: symbolTable.getGlobalVariables()) {
            if (symbolTable.getVariable2type().get(global) == Type.ARRAY) {
                MachineVariable machineVariable = new MachineVariable(new Variable(global, 0), null, Type.ARRAY);
                List<Integer> arrayParams = symbolTable.lookupArrayParam(global);
                int size = 1;
                for (int i = 0; i < arrayParams.size(); i++) {
                    size *= arrayParams.get(i);
                }
                globalAddressMap.put(machineVariable, newAddressIndex(size));
                // initialize as zero
                for (int i = 0; i < size; i++) {
                    programs.add(new MachineCode(PC++, DLX.STW, R0, GLOBAL, globalAddressMap.get(machineVariable) + 4 * i));
                }
            } else {
                MachineVariable machineVariable = new MachineVariable(new Variable(global, 0), null);
                globalAddressMap.put(machineVariable, newAddressIndex(1));
                // initialize as zero
                programs.add(new MachineCode(PC++, DLX.STW, R0, GLOBAL, globalAddressMap.get(machineVariable)));
            }
        }
        // setup main
        functionAddressIndex.put(new FunctionSymbol(-1), 1);
        generate(rootBlocks.get(rootBlocks.size() - 1));

        // setup function
        for (int i = 0; i < rootBlocks.size() - 1; i++) {
            functionAddressIndex.put(rootBlocks.get(i).getCurrentScope(), 1);
            generate(rootBlocks.get(i));
        }
        insertRealPC();
        if (showProgram)
            System.out.println(programs);
        programs.get(0).c = programs.size() * 4 + 4;
        return programs.stream().map(MachineCode::getDLX).mapToInt(Integer::intValue).toArray();
    }
}
