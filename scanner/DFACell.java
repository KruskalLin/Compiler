package scanner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DFACell {
    public final Map<Character, Set<Integer>> strans = new HashMap<>();
    public boolean accept;

    public void addTransition(char input, int next) {
        Set<Integer> set = strans.computeIfAbsent(input, k -> new HashSet<>());
        set.add(next);
    }

    @Override
    public String toString() {
        return "DFACell{" +
                "strans=" + strans.toString() +
                ", accept=" + accept +
                '}';
    }
}
