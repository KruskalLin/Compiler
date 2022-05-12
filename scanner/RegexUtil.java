package scanner;

import error.ErrorMessage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * & = concat
 * | = union
 * @ # = bracket
 * $ ^ = kleene closure bracket
 * ~ = epsilon
 */


public class RegexUtil {

    public static final Map<Integer, String> id2regex = new HashMap<>();
    public static final Map<Integer, String> id2name = new HashMap<>();
    public static final Map<String, Integer> regex2id = new HashMap<>();
    public static final Set<Character> alphabet = new HashSet<>();

    static {
        // we omit the error and EOF here since we treat them specially
        id2regex.put(1, "*");
        id2regex.put(2, "/");
        id2regex.put(3, "+");
        id2regex.put(4, "-");
        id2regex.put(11, "*=");
        id2regex.put(12, "/=");
        id2regex.put(13, "+=");
        id2regex.put(14, "-=");
        id2regex.put(20, "==");
        id2regex.put(21, "!=");
        id2regex.put(22, "<");
        id2regex.put(23, ">=");
        id2regex.put(24, "<=");
        id2regex.put(25, ">");
        id2regex.put(30, ".");
        id2regex.put(31, ",");
        id2regex.put(32, "[");
        id2regex.put(33, "(");
        id2regex.put(34, "]");
        id2regex.put(35, ")");
        id2regex.put(40, ":=");
        id2regex.put(41, "then");
        id2regex.put(42, "do");
        id2regex.put(43, "until");
        id2regex.put(51, "++");
        id2regex.put(52, "--");
        id2regex.put(60, "@0|1|2|3|4|5|6|7|8|9#$0|1|2|3|4|5|6|7|8|9^");
        id2regex.put(61, "@a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z#$_|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9^");
        id2regex.put(62, "_");
        id2regex.put(63, "void");
        id2regex.put(70, ";");
        id2regex.put(77, "let");
        id2regex.put(80, "}");
        id2regex.put(81, "od");
        id2regex.put(82, "fi");
        id2regex.put(90, "else");
        id2regex.put(100, "call");
        id2regex.put(101, "if");
        id2regex.put(102, "while");
        id2regex.put(103, "repeat");
        id2regex.put(104, "return");
        id2regex.put(110, "var");
        id2regex.put(111, "array");
        id2regex.put(112, "function");
        id2regex.put(150, "{");
        id2regex.put(200, "main");
        id2regex.put(254, "//");
        id2regex.put(255, "eof");

        id2name.put(1, "times");
        id2name.put(2, "div");
        id2name.put(3, "plus");
        id2name.put(4, "minus");
        id2name.put(11, "selftimes");
        id2name.put(12, "selfdiv");
        id2name.put(13, "selfplus");
        id2name.put(14, "selfminus");
        id2name.put(20, "eql");
        id2name.put(21, "neq");
        id2name.put(22, "lss");
        id2name.put(23, "geq");
        id2name.put(24, "leq");
        id2name.put(25, "gtr");
        id2name.put(30, "period");
        id2name.put(31, "comma");
        id2name.put(32, "openbracket");
        id2name.put(33, "openparen");
        id2name.put(34, "closebracket");
        id2name.put(35, "closeparen");
        id2name.put(40, "assign");
        id2name.put(41, "then");
        id2name.put(42, "do");
        id2name.put(43, "until");
        id2name.put(51, "increase");
        id2name.put(52, "decrease");
        id2name.put(60, "digits");
        id2name.put(61, "identifier");
        id2name.put(62, "underscore");
        id2name.put(63, "void");
        id2name.put(70, "semi");
        id2name.put(77, "let");
        id2name.put(80, "end");
        id2name.put(81, "od");
        id2name.put(82, "fi");
        id2name.put(90, "else");
        id2name.put(100, "call");
        id2name.put(101, "if");
        id2name.put(102, "while");
        id2name.put(103, "repeat");
        id2name.put(104, "return");
        id2name.put(110, "var");
        id2name.put(111, "array");
        id2name.put(112, "func");
        id2name.put(150, "begin");
        id2name.put(200, "main");
        id2name.put(254, "comment");
        id2name.put(255, "eof");

        for (Map.Entry<Integer, String> entry : id2regex.entrySet()) {
            regex2id.put(entry.getValue(), entry.getKey());
            if (entry.getKey() == 60) {
                alphabet.addAll("0123456789".chars().mapToObj(e->(char)e).collect(Collectors.toSet()));
            } else if (entry.getKey() == 61) {
                alphabet.add('_');
                alphabet.addAll("abcdefghijklmnopqrstuvwxyz0123456789".chars().mapToObj(e->(char)e).collect(Collectors.toSet()));
                alphabet.addAll("abcdefghijklmnopqrstuvwxyz".toUpperCase().chars().mapToObj(e->(char)e).collect(Collectors.toSet()));
            } else {
                alphabet.addAll(entry.getValue().chars().mapToObj(e->(char)e).collect(Collectors.toSet()));
            }
        }
        alphabet.add(' ');
        alphabet.add('\r');
        alphabet.add('\n');
        alphabet.add('\t');
        alphabet.add('\\');

        // other maybe alphabets
        alphabet.add('?');
        alphabet.add(':');

    }

    public static String infix2postfix(String infixRegex) throws ErrorMessage {
        infixRegex = addConcat(infixRegex);
        Stack<Character> operatorStack = new Stack<>();
        StringBuilder postfix = new StringBuilder();
        for (int i = 0; i < infixRegex.length(); i++) {
            Character c = infixRegex.charAt(i);
            if (isCharacter(c)) {
                postfix.append(c);
            } else if (isOperator(c)) {
                if (!operatorStack.empty()
                        && outPriority(c) <= inPriority(operatorStack.peek())) {
                    while (!operatorStack.empty()
                            && outPriority(c) < inPriority(operatorStack.peek())) {
                        postfix.append(operatorStack.peek());
                        operatorStack.pop();
                    }
                }
                operatorStack.push(c);
            } else if (c == '#') {
                while (operatorStack.peek() != '@') {
                    postfix.append(operatorStack.peek());
                    operatorStack.pop();
                    if (operatorStack.empty()) {
                        throw new ErrorMessage("Lexical Analysis", "Valid character input", "Wrong regex input");
                    }
                }
                operatorStack.pop();
            } else if (c == '^') {
                while (operatorStack.peek() != '$') {
                    postfix.append(operatorStack.peek());
                    operatorStack.pop();
                    if (operatorStack.empty()) {
                        throw new ErrorMessage("Lexical Analysis", "Valid character input", "Wrong regex input");
                    }
                }
                postfix.append('^');
                operatorStack.pop();
            }
        }

        while (!operatorStack.empty()) {
            if (operatorStack.peek() == '@' || operatorStack.peek() == '$') {
                throw new ErrorMessage("Lexical Analysis", "Valid character input", "Wrong regex input");
            }
            postfix.append(operatorStack.peek());
            operatorStack.pop();
        }

        return postfix.toString();
    }

    private static String addConcat(String infixRegex) {
        int length = infixRegex.length();
        int i = 0;
        StringBuilder concatStr = new StringBuilder();
        while (i < length) {
            concatStr.append(infixRegex.charAt(i));
            if ((i + 1 < length) && (isCharacter(infixRegex.charAt(i)) || (infixRegex.charAt(i) == '#') || (infixRegex.charAt(i) == '^'))) {
                if (infixRegex.charAt(i + 1) == '@'
                        || infixRegex.charAt(i + 1) == '$'
                        || isCharacter(infixRegex.charAt(i + 1))) {
                    concatStr.append("&");
                }
            }
            i++;
        }
        return concatStr.toString();
    }

    private static boolean isCharacter(Character c) {
        return !c.equals('&') && !c.equals('|') && !c.equals('@') && !c.equals('#') && !c.equals('$') && !c.equals('^');
    }

    private static boolean isOperator(Character c) {
        return c.equals('&') || c.equals('|') || c.equals('@') || c.equals('$');
    }

    private static int inPriority(Character c) {
        return switch (c) {
            case '|' -> 2;
            case '&' -> 4;
            default -> 0;
        };
    }

    private static int outPriority(Character c) {
        return switch (c) {
            case '|' -> 1;
            case '&' -> 3;
            case '@', '$' -> 5;
            default -> 0;
        };
    }

}
