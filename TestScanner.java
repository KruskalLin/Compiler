import error.ErrorMessage;
import scanner.RegexUtil;

import java.io.IOException;

public class TestScanner {
    public static void main(String args[]) {


        try {
            // Redirect System.in from DLX to data file
            Scanner scanner = new Scanner("./resources/code");
            int token;
            while (true) {
                token = scanner.getSym();
                if (token == 255) {
                    System.out.print(RegexUtil.id2name.get(token));
                    break;
                } else {
                    System.out.print(RegexUtil.id2name.get(token));
                    if (token == 60 || token == 61) {
                        System.out.println("[" + scanner.getLastValue() + "]");
                    } else {
                        System.out.println();
                    }
                }
            }
        } catch (IOException | ErrorMessage e) {
            System.err.println(e);
        }
    }

}
