import error.ErrorMessage;

import java.io.IOException;

public class TestParser {
    public static void main(String args[]) {
        try {
            /* Redirect System.in from DLX to data file */
            Parser parser = new Parser("./resources/code");
            while (parser.Derivation()) {}

        } catch (IOException | ErrorMessage e) {
            System.err.println(e);
        }
    }

}
