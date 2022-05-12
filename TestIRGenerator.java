import error.ErrorMessage;
import ir.CONFIG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestIRGenerator {
    public static void main(String args[]) {
        if (args.length < 2) {
            System.err.println("Usage: TestCompiler <code file> (<optimizations> ...)");
            return;
        }


        try {
            /* Redirect System.in from DLX to data file */
            if (args[args.length - 1].equals("ShowDeleted")) {
                CONFIG.SHOW_DELETED = true;
                List<String> argsForIR = new ArrayList<>(Arrays.asList(args));
                argsForIR.remove(argsForIR.size() - 1);
                args = argsForIR.toArray(String[]::new);
            }
            new IRGenerator(args, true);
        } catch (IOException | ErrorMessage e) {
            System.err.println(e);
        }
    }
}
