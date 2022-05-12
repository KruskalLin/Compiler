import java.io.IOException;
import java.util.*;

import static parser.EBNFUtil.*;

import error.ErrorMessage;
import parser.ASTTreeNode;
import parser.Production;

public class Parser {

    private int scannerSym;
    private final Scanner scanner;
    private final Stack<ASTTreeNode> stack;
    private final ASTTreeNode root;

    private void Next() throws IOException, ErrorMessage {
        scannerSym = scanner.getSym();
    } // advance to the next token

    public boolean Derivation() throws IOException, ErrorMessage {
        ASTTreeNode node = stack.pop();
        String symbol = node.getSymbol();

        if (isNumeric(symbol)) {
            node.setValue(scanner.getLastValue());
            int sym = Integer.parseInt(symbol);
            if (scannerSym != sym) {
                throw new ErrorMessage("Parsing", "Valid number input", "Error occurred in parser derivation about numbers");
            }
            if (sym == 255) {
                return false;
            }

            Next();
        } else {
            Production prod = LLTable[nonTerminalMap.get(symbol)][scannerSym];
            if (prod == null) {
                throw new ErrorMessage("Parsing", "Valid grammar input", "Input is not accorded to valid grammar");
            }

            String[] right = prod.getRight().split(" ");
            if (right[0].compareTo("~") != 0) {
                for (int i = right.length - 1; i >= 0; i--) {
                    ASTTreeNode children = new ASTTreeNode(right[i], null);
                    stack.push(children);
                    node.addChild(children);
                }
                node.reverse();
            }
        }
        return true;
    }


    public Parser(String fileName) throws IOException, ErrorMessage {
        scanner = new Scanner(fileName);
        stack = new Stack<>();
        root = new ASTTreeNode("computation", null);
        stack.push(new ASTTreeNode("255", null));
        stack.push(root);
        Next();
    }

    public ASTTreeNode getRoot() {
        return root;
    }


}
