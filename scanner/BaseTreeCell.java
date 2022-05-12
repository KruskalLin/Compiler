package scanner;

public abstract class BaseTreeCell extends RegexTreeCell {

    @Override
    public abstract IntegerPair construct(NFA context);

    public static class CharCell extends BaseTreeCell {
        private final Character c;

        public CharCell(Character c) {
            this.c = c;
        }

        @Override
        public IntegerPair construct(NFA context) {
            int start = context.addCell();
            int end = context.addCell();
            context.addTransition(this.c, start, end);
            return new IntegerPair(start, end);
        }

        @Override
        public String toString() {
            return "" + c;
        }
    }

    public static class EpsilonCell extends BaseTreeCell {

        public EpsilonCell() {
        }

        @Override
        public IntegerPair construct(NFA context) {
            int start = context.addCell();
            int end = context.addCell();
            context.addEpsilon(start, end);
            return new IntegerPair(start, end);
        }

        @Override
        public String toString() {
            return "~";
        }
    }

}
