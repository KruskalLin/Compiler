package scanner;

public abstract class RegexTreeCell {

    public abstract IntegerPair construct(NFA context);

    @Override
    public abstract String toString();
}
