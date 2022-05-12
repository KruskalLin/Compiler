
import error.ErrorMessage;

import java.io.*;
import java.util.*;

public class TestCompiler {
    public static void main(String args[]) {
        if (args.length < 3) {
            System.err.println("Usage: TestCompiler <code file> <data file> <register number>");
            return;
        }

        try {
            // Redirect System.in from DLX to data file
            InputStream origIn = System.in,
                        newIn = new BufferedInputStream(
                                new FileInputStream(args[1]));

            System.setIn(newIn);

            Compiler p = new Compiler(args);
            int prog[] = p.getProgram();
            if (prog == null) {
                System.err.println("Error compiling program!");
                return;
            }

            DLX.load(prog);
            DLX.execute();

            System.setIn(origIn);
            newIn.close();
        } catch (IOException | ErrorMessage e) {
            System.err.println(e);
        }
    }

}

