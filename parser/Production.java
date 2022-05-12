package parser;

public class Production {
    private final String left;
    private final String right;


    public Production(String left, String right) {
        this.left = left;
        this.right = right;
    }

    public String getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "Production{" +
                "left='" + left + '\'' +
                ", right='" + right + '\'' +
                '}';
    }
}
