package ir;

import error.ErrorMessage;

import java.util.*;

public class SymbolTable {

    private List<String> globalVariables;
    private List<Type> globalVariableTypes;
    private Map<String, Type> variable2type;
    private List<FunctionSymbol> functionSymbols;
    private Map<String, List<Integer>> arrayParamMap;

    public FunctionSymbol current;

    public SymbolTable() {
        globalVariables = new ArrayList<>();
        globalVariableTypes = new ArrayList<>();
        variable2type = new HashMap<>();
        functionSymbols = new ArrayList<>();
        arrayParamMap = new HashMap<>();
    }

    public List<FunctionSymbol> getFunctionSymbols() {
        return functionSymbols;
    }

    public List<String> getGlobalVariables() {
        return globalVariables;
    }

    public void insertFunctionSymbol(FunctionSymbol scope) {
        for (FunctionSymbol functionSymbol : functionSymbols) {
            assert (!functionSymbol.isEqual(scope.getFunctionName(), scope.getParams().size())) : "Function " + scope.getFunctionName() + " redefine";
        }
        functionSymbols.add(scope);
    }

    public Type lookupType(String name) throws ErrorMessage {
        if (current != null) {
            Type type = current.lookupType(name);
            if (type != null) {
                return type;
            }
        }
        for (int i = 0; i < globalVariables.size(); i++) {
            if (globalVariables.get(i).equals(name)) {
                return globalVariableTypes.get(i);
            }
        }
        throw new ErrorMessage("Type Checking", "Variable " + name,"The variable " + name + " does not exist");
    }

    public FunctionSymbol lookupFunctionSymbol(String name, int size) throws ErrorMessage {
        for (FunctionSymbol functionSymbol : functionSymbols) {
            if (functionSymbol.isEqual(name, size)) {
                return functionSymbol;
            }
        }
        throw new ErrorMessage("Type Checking", "Function " + name,"Calling the non-existed function " + name + " with parameter size " + size);
    }

    public FunctionSymbol lookupFunctionSymbol(int blockID) throws ErrorMessage {
        for (FunctionSymbol functionSymbol : functionSymbols) {
            if (functionSymbol.getEntryBlockID() == blockID) {
                return functionSymbol;
            }
        }
        throw new ErrorMessage("Type Checking", "BlockID " + blockID,"Calling the non-existed block " + blockID);
    }

    public List<Integer> lookupArrayParam(String ident) throws ErrorMessage {
        List<Integer> arrayParams = null;
        if (current != null) {
            arrayParams = current.lookupArrayParams(ident);
        }
        if (arrayParams == null)
            arrayParams = arrayParamMap.get(ident);

        if (arrayParams == null)
            throw new ErrorMessage("Type Checking", "Array " + ident,"No such identifier " + ident);
        return arrayParams;
    }

    public void insertGlobal(String name, Type type) {
        for (String globalVariable : globalVariables) {
            assert (!globalVariable.equals(name)) : "Variable " + name + " redefine";
        }
        this.globalVariables.add(name);
        this.globalVariableTypes.add(type);
        this.variable2type.put(name, type);
    }

    public Map<String, Type> getVariable2type() {
        return variable2type;
    }

    public void insertArrayParams(String ident, List<Integer> params) {
        this.arrayParamMap.put(ident, params);
    }

    public void setCurrent(FunctionSymbol current) {
        this.current = current;
    }
    // if its null it is the main block
    public FunctionSymbol getCurrent() {
        return current;
    }
}
