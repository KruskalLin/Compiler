package scanner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NFACell {
    public final Set<Integer> etrans = new HashSet<>();
    public final Map<Character, Set<Integer>> strans = new HashMap<>();

    public void addTransition(char input, int next) {
        Set<Integer> set = strans.computeIfAbsent(input, k -> new HashSet<>());
        set.add(next);
    }

    public void addEpsilon(int next) {
        etrans.add(next);
    }


}