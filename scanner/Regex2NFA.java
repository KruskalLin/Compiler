package scanner;

import error.ErrorMessage;

import java.util.Stack;

public class Regex2NFA {

    public static RegexTreeCell regex2Tree(String regex) throws ErrorMessage {
        String postfix = RegexUtil.infix2postfix(regex);
        Stack<RegexTreeCell> s = new Stack<>();
        for (int i = 0; i < postfix.length(); i++) {
            switch (postfix.charAt(i)) {
                case '&' -> {
                    RegexTreeCell b1 = s.pop();
                    RegexTreeCell a1 = s.pop();
                    RegexTreeCell cell1 = new BiTreeCell.ConcatenationCell(a1, b1);
                    s.push(cell1);
                }
                case '|' -> {
                    RegexTreeCell b2 = s.pop();
                    RegexTreeCell a2 = s.pop();
                    RegexTreeCell cell2 = new BiTreeCell.UnionCell(a2, b2);
                    s.push(cell2);
                }
                case '^' -> {
                    RegexTreeCell r1 = s.pop();
                    RegexTreeCell cell3 = new UniTreeCell.KleeneClosureCell(r1);
                    s.push(cell3);
                }
                case '~' -> {
                    RegexTreeCell cell4 = new BaseTreeCell.EpsilonCell();
                    s.push(cell4);
                }
                default -> {
                    RegexTreeCell cell5 = new BaseTreeCell.CharCell(postfix.charAt(i));
                    s.push(cell5);
                }
            }
        }
        return s.pop();
    }

    public static NFA regex2NFA(String regex) throws ErrorMessage {
        NFA context = new NFA();
        RegexTreeCell cell = regex2Tree(regex);
        cell.construct(context);
        return context;
    }

}
