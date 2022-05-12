package scanner;

import java.util.*;

public class DFA {
    public final List<DFACell> cells;
    public final List<Set<Integer>> states;
    public final List<Character> alphabet;
    Integer[][] dtrans;
    Integer acceptNum;

    public DFA(List<Set<Integer>> states, List<Character> alphabet, Integer[][] dtrans, Integer acceptNum) {
        this.states = states;
        this.alphabet = alphabet;
        this.dtrans = dtrans;
        this.acceptNum = acceptNum;
        this.cells = new ArrayList<>();

        for (Set<Integer> state : states) {
            addCell(state.contains(acceptNum));
        }

        for (int i = 0; i < states.size(); i++) {
            for (int j = 0; j < alphabet.size(); j++) {
                if (dtrans[i][j] != -1) {
                    addTransition(alphabet.get(j), i, dtrans[i][j]);
                }
            }
        }

    }

    private void addCell(boolean accept) {
        DFACell state = new DFACell();
        state.accept = accept;
        cells.add(state);
    }

    private void addTransition(char input, int current, int next) {
        cells.get(current).addTransition(input, next);
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

    public boolean search(String regex) {
        Set<Integer> start = new HashSet<>();
        start.add(0);
        for (int i = 0; i < regex.length(); i++) {
            Set<Integer> set = move(start, regex.charAt(i));
            if (set.size() == 0) {
                return false;
            } else if (i == regex.length() - 1) {
                for (int j : set) {
                    if (cells.get(j).accept) {
                        return true;
                    }
                }
            }
            start = new HashSet<>(set);
        }
        return false;
    }

}
