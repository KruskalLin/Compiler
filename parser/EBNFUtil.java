package parser;

import error.ErrorMessage;

import java.util.*;

public class EBNFUtil {
    public static final Map<String, List<String>> productionStringMap = new HashMap<>();
    public static final Map<String, List<Production>> productionMap = new HashMap<>();
    public static final Map<String, Integer> nonTerminalMap = new HashMap<>();
    public static final Map<String, Set<Integer>> firstSetMap = new HashMap<>();
    public static final Map<String, Set<Integer>> followSetMap = new HashMap<>();

    public static final Production[][] LLTable = new Production[64][256];

    static {
        productionStringMap.put("relOp", new ArrayList<>(Arrays.asList("20", "21", "22", "23", "24", "25")));
        productionStringMap.put("shortHandOp", new ArrayList<>(Arrays.asList("11", "12", "13", "14")));
        productionStringMap.put("uniOp", new ArrayList<>(Arrays.asList("51", "52")));
        productionStringMap.put("ident", new ArrayList<>(List.of("61")));
        productionStringMap.put("number", new ArrayList<>(Arrays.asList("4 60", "60")));
        productionStringMap.put("designator", new ArrayList<>(List.of("ident designatorClosure")));
        productionStringMap.put("designatorClosure", new ArrayList<>(Arrays.asList("32 expression 34 designatorClosure", "~")));

        productionStringMap.put("factor", new ArrayList<>(Arrays.asList("designator", "number", "33 expression 35", "funcCall")));
        productionStringMap.put("term", new ArrayList<>(List.of("factor factorClosure")));
        productionStringMap.put("factorClosure", new ArrayList<>(Arrays.asList("1 factor factorClosure", "2 factor factorClosure", "~")));

        productionStringMap.put("expression", new ArrayList<>(List.of("term termClosure")));
        productionStringMap.put("termClosure", new ArrayList<>(Arrays.asList("3 term termClosure", "4 term termClosure", "~")));

        productionStringMap.put("relation", new ArrayList<>(List.of("expression relOp expression")));
        productionStringMap.put("assignment", new ArrayList<>(List.of("77 designator subAssignment")));
        productionStringMap.put("subAssignment", new ArrayList<>(Arrays.asList("40 expression", "shortHandOp expression", "uniOp")));

        productionStringMap.put("funcCall", new ArrayList<>(List.of("100 ident 33 expressionClosure 35")));
        productionStringMap.put("expressionClosure", new ArrayList<>(List.of("expression expressionSubClosure", "~")));
        productionStringMap.put("expressionSubClosure", new ArrayList<>(Arrays.asList("31 expression expressionSubClosure", "~")));

        productionStringMap.put("ifStatement", new ArrayList<>(List.of("101 relation 41 statSequence elseStatement 82")));
        productionStringMap.put("elseStatement", new ArrayList<>(Arrays.asList("90 statSequence", "~")));

        productionStringMap.put("whileStatement", new ArrayList<>(List.of("102 relation 42 statSequence 81")));
        productionStringMap.put("repeatStatement", new ArrayList<>(List.of("103 statSequence 43 relation")));
        productionStringMap.put("returnStatement", new ArrayList<>(List.of("104 expressionStatement")));
        productionStringMap.put("expressionStatement", new ArrayList<>(Arrays.asList("expression", "~")));

        productionStringMap.put("statement", new ArrayList<>(Arrays.asList("assignment", "funcCall", "ifStatement", "whileStatement", "repeatStatement", "returnStatement")));
        productionStringMap.put("statSequence", new ArrayList<>(List.of("statement statSequenceClosure")));
        productionStringMap.put("statSequenceClosure", new ArrayList<>(Arrays.asList("70 statSubSequenceClosure", "~")));
        productionStringMap.put("statSubSequenceClosure", new ArrayList<>(Arrays.asList("statement statSequenceClosure", "~")));

        productionStringMap.put("typeDecl", new ArrayList<>(Arrays.asList("110", "111 32 number 34 numberClosure")));
        productionStringMap.put("numberClosure", new ArrayList<>(Arrays.asList("32 number 34 numberClosure", "~")));

        productionStringMap.put("varDecl", new ArrayList<>(List.of("typeDecl ident identClosure 70")));
        productionStringMap.put("identClosure", new ArrayList<>(Arrays.asList("31 ident identClosure", "~")));

        productionStringMap.put("funcDecl", new ArrayList<>(Arrays.asList("112 ident formalParam funcBody 70", "63 112 ident formalParam funcBody 70")));
        productionStringMap.put("formalParam", new ArrayList<>(List.of("33 formalParamStatement 35")));
        productionStringMap.put("formalParamStatement", new ArrayList<>(Arrays.asList("ident identClosure", "~")));

        productionStringMap.put("funcBody", new ArrayList<>(List.of("150 funcBodyStatement 80")));
        productionStringMap.put("funcBodyStatement", new ArrayList<>(List.of("varDecl funcBodyStatement", "statSequence", "~")));

        productionStringMap.put("computation", new ArrayList<>(List.of("200 varDeclClosure funcDeclClosure 150 statSequence 80 30")));
        productionStringMap.put("varDeclClosure", new ArrayList<>(Arrays.asList("varDecl varDeclClosure", "~")));
        productionStringMap.put("funcDeclClosure", new ArrayList<>(Arrays.asList("funcDecl funcDeclClosure", "~")));

        for (Map.Entry<String, List<String>> entry : productionStringMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            List<Production> productions = new ArrayList<>();
            for (String value : values) {
                productions.add(new Production(key, value));
            }
            productionMap.put(key, productions);
        }

        nonTerminalMap.put("computation", 0);
        nonTerminalMap.put("relOp", 1);
        nonTerminalMap.put("shortHandOp", 2);
        nonTerminalMap.put("uniOp", 3);
        nonTerminalMap.put("ident", 4);
        nonTerminalMap.put("number", 5);
        nonTerminalMap.put("designator", 6);
        nonTerminalMap.put("factor", 7);
        nonTerminalMap.put("term", 8);
        nonTerminalMap.put("expression", 9);
        nonTerminalMap.put("relation", 10);
        nonTerminalMap.put("assignment", 11);
        nonTerminalMap.put("funcCall", 12);
        nonTerminalMap.put("ifStatement", 13);
        nonTerminalMap.put("whileStatement", 14);
        nonTerminalMap.put("repeatStatement", 15);
        nonTerminalMap.put("returnStatement", 16);
        nonTerminalMap.put("statement", 17);
        nonTerminalMap.put("statSequence", 18);
        nonTerminalMap.put("typeDecl", 19);
        nonTerminalMap.put("varDecl", 20);
        nonTerminalMap.put("funcDecl", 21);
        nonTerminalMap.put("formalParam", 22);
        nonTerminalMap.put("funcBody", 23);

        nonTerminalMap.put("designatorClosure", 24);
        nonTerminalMap.put("factorClosure", 25);
        nonTerminalMap.put("termClosure", 26);
        nonTerminalMap.put("subAssignment", 27);
        nonTerminalMap.put("expressionClosure", 28);
        nonTerminalMap.put("expressionSubClosure", 29);
        nonTerminalMap.put("elseStatement", 30);
        nonTerminalMap.put("expressionStatement", 31);
        nonTerminalMap.put("statSequenceClosure", 32);
        nonTerminalMap.put("statSubSequenceClosure", 33);

        nonTerminalMap.put("numberClosure", 34);
        nonTerminalMap.put("identClosure", 35);
        nonTerminalMap.put("formalParamStatement", 36);
        nonTerminalMap.put("funcBodyStatement", 37);
        nonTerminalMap.put("varDeclClosure", 38);
        nonTerminalMap.put("funcDeclClosure", 39);

        constructFirstSet();

        // eliminate recursive follow
        Set<Integer> followSet = getFollowSet("statSequence");
        followSetMap.put("statSequenceClosure", followSet);
        followSetMap.put("statSubSequenceClosure", followSet);

        constructFollowSet();
        try {
            constructFirstTable();
        } catch (ErrorMessage e) {
            e.printStackTrace();
        }
    }

    public static void constructFirstTable() throws ErrorMessage {
        for (Map.Entry<String, List<Production>> entry : productionMap.entrySet()) {
            String key = entry.getKey();
            int row = nonTerminalMap.get(key);
            List<Production> values = entry.getValue();
            for (Production value : values) {
                String[] right = value.getRight().split(" ");
                if (isNumeric(right[0])) {
                    int terminal = Integer.parseInt(right[0]);
                    if (LLTable[row][terminal] != null) {
                        throw new ErrorMessage("Parsing", "Valid LL(1) grammar", "First-First conflict. No LL(1) language");
                    }
                    LLTable[row][terminal] = value;
                } else {
                    Set<Integer> firstSet = getFirstSetFromProduction(key, right, 0);
                    for (int terminal : firstSet) {
                        if (terminal != -1) {
                            if (LLTable[row][terminal] != null) {
                                throw new ErrorMessage("Parsing", "Valid LL(1) grammar", "First-First conflict. No LL(1) language");
                            }
                            LLTable[row][terminal] = value;
                        }
                    }
                    if (firstSet.contains(-1)) {
                        Set<Integer> followSet = getFollowSet(key);
                        for (int terminal : followSet) {
                            if (LLTable[row][terminal] != null) {
                                throw new ErrorMessage("Parsing", "Valid LL(1) grammar", "First-Follow conflict. No LL(1) language");
                            }
                            LLTable[row][terminal] = value;
                        }
                    }
                }
            }
        }
    }

    public static void constructFirstSet() {
        for (Map.Entry<String, List<Production>> entry : productionMap.entrySet()) {
            String key = entry.getKey();
            if (!firstSetMap.containsKey(key)) {
                firstSetMap.put(key, getFirstSet(key));
            }
        }
    }

    public static Set<Integer> getFirstSet(String start) {
        if (firstSetMap.containsKey(start)) {
            return firstSetMap.get(start);
        }
        Set<Integer> firstSet = new HashSet<>();
        List<Production> values = productionMap.get(start);
        for (Production value : values) {
            String[] right = value.getRight().split(" ");
            firstSet.addAll(getFirstSetFromProduction(start, right, 0));
        }
        firstSetMap.put(start, firstSet);
        return firstSet;
    }

    public static Set<Integer> getFirstSetFromProduction(String left, String[] right, int i) {
        Set<Integer> firstSet = new HashSet<>();
        if (right.length == 1 && isEpsilon(right[0])) {
            firstSet.add(-1);
            return firstSet;
        }
        while (i < right.length) {
            if (isNumeric(right[i])) {
                firstSet.add(Integer.parseInt(right[i]));
                return firstSet;
            } else {
                if (left.compareTo(right[i]) == 0) {
                    i++;
                    continue;
                }
                Set<Integer> firstSubset = getFirstSet(right[i]);
                firstSet.addAll(firstSubset);
                if (!firstSet.contains(-1)) {
                    return firstSet;
                } else {
                    firstSet.remove(-1);
                    i++;
                }
            }
        }
        firstSet.add(-1);
        return firstSet;
    }

    public static void constructFollowSet() {
        for (Map.Entry<String, List<Production>> entry : productionMap.entrySet()) {
            String key = entry.getKey();
            if (firstSetMap.get(key).contains(-1)) {
                if (!followSetMap.containsKey(key))
                    followSetMap.put(key, getFollowSet(key));
            }
        }
    }

    public static Set<Integer> getFollowSet(String left) {
        if (followSetMap.containsKey(left))
            return followSetMap.get(left);

        Set<Integer> followSet = new HashSet<>();
        if (left.compareTo("computation") == 0) {
            followSet.add(255);
            return followSet;
        }
        for (Map.Entry<String, List<Production>> entry : productionMap.entrySet()) {
            String key = entry.getKey();
            List<Production> values = entry.getValue();
            for (Production value : values) {
                String[] right = value.getRight().split(" ");
                int i = 0;
                while (i < right.length) {
                    if (right[i].compareTo(left) == 0) {
                        if (i + 1 == right.length && left.compareTo(key) != 0) {
                            followSet.addAll(getFollowSet(key));
                        } else if (i + 1 < right.length) {
                            if (isNumeric(right[i + 1])) {
                                followSet.add(Integer.parseInt(right[i + 1]));
                            } else {
                                while (i < right.length) {
                                    if (i + 1 == right.length && left.compareTo(key) != 0) {
                                        followSet.addAll(getFollowSet(key));
                                        break;
                                    } else if (i + 1 < right.length) {
                                        if (isNumeric(right[i + 1])) {
                                            followSet.add(Integer.parseInt(right[i + 1]));
                                            break;
                                        } else {
                                            Set<Integer> firstSet = getFirstSet(right[i + 1]);
                                            followSet.addAll(firstSet);
                                            if (!firstSet.contains(-1)) {
                                                break;
                                            }
                                        }
                                    }
                                    i++;
                                }
                            }
                        }
                    }
                    i++;
                }
            }
        }
        followSet.remove(-1);
        followSetMap.put(left, followSet);
        return followSet;
    }

    public static boolean isNumeric(String candidate) {
        if (candidate.equals(""))
            return false;
        int i = 0;
        if (candidate.charAt(0) == '-')
            i = 1;
        for (; i < candidate.length(); i++) {
            if (!Character.isDigit(candidate.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEpsilon(String candidate) {
        return candidate.compareTo("~") == 0;
    }
}
