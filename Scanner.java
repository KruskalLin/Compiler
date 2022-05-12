import java.io.IOException;
import java.util.*;

import error.ErrorMessage;
import scanner.DFA;
import scanner.Regex2NFA;
import scanner.RegexUtil;

public class Scanner {
    /*moved up from FileReader*/

    private final FileReader reader;
    private Character inputSym; // the current character on the input
    private final StringBuilder substring;
    private static Map<String, DFA> matcher;
    private int lastToken = -1;
    private String lastValue;

    private void Next() throws IOException {
        inputSym = reader.getSym();
    } // advance to the next character

    // Error == 0, EOF == 255
    /* symmetrical to the changed FileReader class */
    public int getSym() throws IOException, ErrorMessage {
        if (substring.length() == 0) {
            lastToken = -1;
        }

        if (inputSym == null) {
            lastToken = 255;
            substring.setLength(0);
        }

        while (inputSym != null) {
            if (!RegexUtil.alphabet.contains(inputSym)) {
                throw new ErrorMessage("Lexical Analysis", "Valid character", "Input character " + inputSym + " not in alphabet!");
            }
            substring.append(inputSym);
            int localToken = -1;
            boolean match = false;
            for (String reg : matcher.keySet()) {
                boolean result = matcher.get(reg).search(substring.toString());
                match |= result;
                if (result) {
                    if (localToken == -1 || localToken == 61) {
                        localToken = RegexUtil.regex2id.get(reg);
                    }
                }
            }

            if (lastToken == 254) { // comment
                while (inputSym != null && inputSym != '\n') {
                    Next();
                }
                Next();
                substring.setLength(0);
                return getSym();
            }

            if (match) {
                Next();
                lastToken = localToken;
                lastValue = substring.toString();
            } else if (lastToken != -1) {
                substring.setLength(0);
                return lastToken;
            } else if (substring.length() == 1 && (inputSym == '\t' || inputSym == ' ' || inputSym == '\n' || inputSym == '\r')) {
                Next();
                substring.setLength(0);
                return getSym();
            } else {
                Next();
            }
        }
        return lastToken;
    } // return current and advance to the next token on the input

    public String getLastValue() {
        return lastValue;
    }

    public Scanner(String fileName) throws IOException, ErrorMessage {
        reader = new FileReader(fileName);
        substring = new StringBuilder();
        matcher = new HashMap<>();
        for (String reg : RegexUtil.regex2id.keySet()) {
            matcher.put(reg, Regex2NFA.regex2NFA(reg).NFA2DFA());
        }

        Next();
    } // constructor: open file and scan the first token into 'sym'
}