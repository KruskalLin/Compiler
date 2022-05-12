package ir;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RegisterAllocation {
    private int registerNumber;
    private Stack<RegisterGraphNode> stack;

    public RegisterAllocation(int registers) {
        this.registerNumber = registers;
        stack = new Stack<>();
    }

    public void ChaitinBrigg(Set<RegisterGraphNode> nodes) {
        stack.clear();
        while (stack.size() < nodes.size()) {
            int n_max = -1;
            RegisterGraphNode max_node = null;
            boolean find = false;
            for (RegisterGraphNode node : nodes) {
                if (node.isDeleted())
                    continue;
                Set<RegisterGraphNode> neighbors = node.getNeighbors();
                int n = 0;
                for (RegisterGraphNode neighbor : neighbors) {
                    if (!neighbor.isDeleted()) {
                        n++;
                    }
                }
                if (n > n_max) {
                    n_max = n;
                    max_node = node;
                }
                if (n < this.registerNumber) {
                    stack.add(node);
                    node.setDeleted(true);
                    find = true;
                    break;
                }
            }
            if (!find) {
                max_node.setTroublesome(true);
                max_node.setDeleted(true);
                stack.add(max_node);
            }
        }
        while (!stack.empty()) {
            RegisterGraphNode node = stack.pop();
            node.setDeleted(false);
            int color = getNextColor(node.getNeighbors());
            if (color < 0) {
                node.setSpilled(true);
                node.setColor(99);
            } else {
                node.setColor(color);
            }
        }
    }

    public int getNextColor(Set<RegisterGraphNode> nodes) {
        List<Integer> colors = IntStream.range(0, this.registerNumber).boxed().collect(Collectors.toList());
        List<Integer> nodeColors = nodes.stream().map(RegisterGraphNode::getColor).collect(Collectors.toList());
        colors.removeAll(nodeColors);
        if (colors.size() > 0)
            return colors.get(0);
        else
            return -1;
    }
}
