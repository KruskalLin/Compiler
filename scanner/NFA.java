package scanner;

import java.util.*;


public class NFA {
    public final List<NFACell> cells = new ArrayList<>();
    public final Set<Character> alphabet = new HashSet<>();

    public int addCell() {
        NFACell state = new NFACell();
        cells.add(state);
        return cells.size() - 1;
    }

    public void addTransition(char input, int current, int next) {
        alphabet.add(input);
        cells.get(current).addTransition(input, next);
    }

    public void addEpsilon(int current, int next) {
        cells.get(current).addEpsilon(next);
    }

    public Set<Integer> eclosure(int stateNum) {
        Set<Integer> eclosure = new HashSet<>();
        Stack<Integer> stack = new Stack<>();
        eclosure.add(stateNum);
        stack.push(stateNum);
        while (!stack.empty()) {
            int state = stack.pop();
            Set<Integer> etrans = cells.get(state).etrans;
            for (int i : etrans) {
                if (!eclosure.contains(i)) {
                    eclosure.add(i);
                    stack.push(i);
                }
            }
        }
        return eclosure;
    }

    public Set<Integer> eclosure(Set<Integer> states) {
        Set<Integer> eclosures = new HashSet<>();
        for (int state : states) {
            eclosures.addAll(eclosure(state));
        }
        return eclosures;
    }

    public Set<Integer> move(Set<Integer> stateNums, char input) {
        Set<Integer> move = new HashSet<>();
        for (int stateNum : stateNums) {
            Set<Integer> nexts = cells.get(stateNum).strans.get(input);
            if (nexts != null) {
                move.addAll(nexts);
            }
        }
        return move;
    }

    public DFA NFA2DFA() {
        Integer[][] dtran = new Integer[cells.size()][this.alphabet.size()];
        for (int i = 0; i < cells.size(); i++) {
            for (int j = 0; j < this.alphabet.size(); j++) {
                dtran[i][j] = -1;
            }
        }
        List<Character> alphabet = new ArrayList<>(this.alphabet);
        List<Set<Integer>> states = new ArrayList<>();
        Set<Integer> e0 = eclosure(0);
        states.add(e0);
        int stateCursor = 0;
        while (stateCursor < states.size()) {
            Set<Integer> notLabeled = states.get(stateCursor);
            int alpahbetCount = -1;
            for (char i : alphabet) {
                alpahbetCount++;
                Set<Integer> u = eclosure(move(notLabeled, i));
                if (!u.isEmpty()) {
                    if (!states.contains(u)) {
                        states.add(u);
                    }
                    dtran[stateCursor][alpahbetCount] = states.indexOf(u);
                }
            }
            stateCursor++;
        }
        return new DFA(states, alphabet, dtran, cells.size() - 1);
    }
}