import error.ErrorMessage;

import java.io.*;

public class FileReader {

    private Reader reader;

    public Character getSym() throws IOException {
        int r = reader.read();
        if (r != -1) {
            return (char) r;
        } else {
            return null;
        }
    } // return current and advance to the next character on the input public void Error(String errorMsg); // signal an error message

    public FileReader(String fileName){
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    } // constructor: open file
}