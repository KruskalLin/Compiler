package ir;

import error.ErrorMessage;

import java.util.*;

public class TypeChecker {
    private static Set<String> expressionSet = new HashSet<>(Arrays.asList("ADD", "SUB", "MUL", "DIV", "CMP"));

    private static Set<String> voidSet = new HashSet<>(Arrays.asList("BEQ", "BNE", "BLT", "BGE", "BGT", "BLE", "BRA", "READ", "WRITENL"));

    public Type expression(String ops, Type src, Type dst, int lineCount) throws ErrorMessage {
        if (expressionSet.contains(ops)) {
            assert (src == dst && src == Type.INT) : "Type checking failed with operation " + ops + ", source type " + src + ", target type " + dst + " at IR line " + lineCount;
            return Type.INT;
        } else if (voidSet.contains(ops)) {
            if (ops.equals("READ"))
                return Type.INT;
            return Type.VOID;
        }
        throw new ErrorMessage("Type Checking", "Valid operation", "Unknown operation " + ops);
    }

}
