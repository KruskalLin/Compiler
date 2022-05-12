package ir;

import java.util.*;

public class AvailableExpression {
    private final int lineCount;
    private final String ops;
    private final Variable src;
    private final Variable dst;

    public AvailableExpression(int lineCount, String ops, Variable src, Variable dst) {
        this.lineCount = lineCount;
        this.ops = ops;
        this.src = src;
        this.dst = dst;
    }

    public int getLineCount() {
        return lineCount;
    }

    public String getOps() {
        return ops;
    }

    public Variable getSrc() {
        return src;
    }

    public Variable getDst() {
        return dst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvailableExpression)) return false;
        AvailableExpression that = (AvailableExpression) o;
        return getOps().equals(that.getOps()) && getSrc().equals(that.getSrc()) && getDst().equals(that.getDst());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOps(), getSrc(), getDst());
    }

    @Override
    public String toString() {
        return "AvailableExpression{" +
                "lineCount=" + lineCount +
                ", ops='" + ops + '\'' +
                ", src=" + src +
                ", dst=" + dst +
                '}';
    }
}
