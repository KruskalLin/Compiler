package ir;

import java.util.*;

public class FunctionSymbol {
    // every tree is a scope with local variables and their types
    private int entryBlockID;
    private String functionName;
    private List<String> variables;
    private List<Type> types;

    private List<String> params; // must be Type.INT

    private Type functionType;

    private List<Type> returnTypes;

    private Map<String, List<Integer>> arrayParamMap;

    private List<String> realParams;
    private List<String> realGlobals;

    private SymbolTable symbolTable;

    public FunctionSymbol(SymbolTable symbolTable, Type type, String name, List<String> parameters, Integer id) {
        this.symbolTable = symbolTable;
        functionType = type;
        functionName = name;
        params = parameters;
        entryBlockID = id;
        variables = new ArrayList<>();
        types = new ArrayList<>();
        returnTypes = new ArrayList<>();
        arrayParamMap = new HashMap<>();

    }

    public FunctionSymbol(int entryBlockID) {
        this.entryBlockID = entryBlockID;
    }

    public void setEntryBlockID(int entryBlockID) {
        this.entryBlockID = entryBlockID;
    }

    public void insertLocal(String name, Type type) {
        for (int i = 0; i < variables.size(); i++) {
            assert (!variables.get(i).equals(name)) : "Variable " + name + " redefine";
        }
        for (int i = 0; i < params.size(); i++) {
            assert (!params.get(i).equals(name)) : "Variable" + name + " is conflicted with params";
        }
        this.variables.add(name);
        this.types.add(type);
    }

    public void insertReturnType(Type type) {
        for (int i = 0; i < returnTypes.size(); i++) {
            assert (returnTypes.get(i) == type) : "Incoherent return type within function " + functionName;
        }
        this.returnTypes.add(type);
    }

    public void insertArrayParams(String ident, List<Integer> arrayParams) {
        this.arrayParamMap.put(ident, arrayParams);
    }

    public boolean isEqual(String name, int size) {
        if (functionName.equals(name) && params.size() == size) {
            return true;
        }
        return false;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Type getFunctionType() {
        return functionType;
    }

    public Integer getEntryBlockID() {
        return entryBlockID;
    }

    public List<String> getVariables() {
        return variables;
    }

    public List<String> getParams() {
        return params;
    }

    public List<Type> getReturnTypes() {
        return returnTypes;
    }

    public Type lookupType(String name) {
        for (int i = 0; i < variables.size(); i++) {
            if (variables.get(i).equals(name)) {
                return types.get(i);
            }
        }

        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).equals(name)) {
                return Type.INT;
            }
        }
        return null;
    }

    public List<Integer> lookupArrayParams(String ident) {
        return this.arrayParamMap.get(ident);
    }


    public List<String> getRealParams() {
        if (realParams == null) {
            realParams = new ArrayList<>(params);
            realParams.removeAll(variables);
        }
        return realParams;
    }

    public List<Boolean> getParamRealIndices() {
        List<Boolean> indices = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            if (variables.contains(params.get(i))) {
                // shadow
                indices.add(false);
            } else {
                indices.add(true);
            }
        }
        return indices;
    }

    public List<String> getRealGlobalVariables() {
        if (realGlobals == null) {
            realGlobals = new ArrayList<>(symbolTable.getGlobalVariables());
            realGlobals.removeAll(params);
            realGlobals.removeAll(variables);
        }
        return realGlobals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FunctionSymbol)) return false;
        FunctionSymbol that = (FunctionSymbol) o;
        return getEntryBlockID() == that.getEntryBlockID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntryBlockID());
    }
}
