package error;

public class ErrorMessage extends Exception {

    public ErrorMessage(String what, String expected, String actual) {
        super("Error occurred in " + what + ". Expected: " + expected + "; But received: " + actual);
    }


}