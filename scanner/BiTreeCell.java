package scanner;


public abstract class BiTreeCell extends RegexTreeCell {
    public RegexTreeCell former;
    public RegexTreeCell latter;

    public BiTreeCell(RegexTreeCell former, RegexTreeCell latter) {
        this.former = former;
        this.latter = latter;
    }

    public static class UnionCell extends BiTreeCell {
        public UnionCell(RegexTreeCell former, RegexTreeCell latter) {
            super(former, latter);
        }

        @Override
        public IntegerPair construct(NFA context) {
            int start = context.addCell();
            IntegerPair first = former.construct(context);
            IntegerPair second = latter.construct(context);
            int end = context.addCell();
            context.addEpsilon(start, first.getFormer());
            context.addEpsilon(start, second.getFormer());
            context.addEpsilon(first.getLatter(), end);
            context.addEpsilon(second.getLatter(), end);
            return new IntegerPair(start, end);
        }

        @Override
        public String toString() {
            return "(" + former.toString() + "|" + latter.toString() + ")";
        }
    }

    public static class ConcatenationCell extends BiTreeCell {

        public ConcatenationCell(RegexTreeCell former, RegexTreeCell latter) {
            super(former, latter);
        }

        @Override
        public IntegerPair construct(NFA context) {
            IntegerPair first = former.construct(context);
            IntegerPair second = latter.construct(context);
            context.addEpsilon(first.getLatter(), second.getFormer());
            return new IntegerPair(first.getFormer(), second.getLatter());
        }

        @Override
        public String toString() {
            return "(" + former.toString() + latter.toString() + ")";
        }
    }

}
