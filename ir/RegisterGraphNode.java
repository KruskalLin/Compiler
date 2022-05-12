package ir;

import java.util.*;

public class RegisterGraphNode {
    private LiveVariable variable;
    private Set<RegisterGraphNode> neighbors;
    private boolean deleted;
    private boolean troublesome;
    private boolean spilled;
    private Integer color;

    public RegisterGraphNode(LiveVariable variable) {
        this.variable = variable;
        this.neighbors = new HashSet<>();
        this.deleted = false;
        this.troublesome = false;
        this.spilled = false;
        this.color = -1;
    }

    public LiveVariable getVariable() {
        return variable;
    }

    public boolean isTroublesome() {
        return troublesome;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Set<RegisterGraphNode> getNeighbors() {
        return neighbors;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setTroublesome(boolean troublesome) {
        this.troublesome = troublesome;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public boolean isSpilled() {
        return spilled;
    }

    public void setSpilled(boolean spilled) {
        this.spilled = spilled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegisterGraphNode)) return false;
        RegisterGraphNode graphNode = (RegisterGraphNode) o;
        return getVariable().equals(graphNode.getVariable());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVariable());
    }

    @Override
    public String toString() {
        return variable.toString();
    }
}
