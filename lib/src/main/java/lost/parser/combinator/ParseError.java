package lost.parser.combinator;

public class ParseError extends RuntimeException {

    public ParseError(String message) {
        super(message, null, false, false);
    }

    public ParseError(String message, Throwable cause) {
        super(message, cause, false, false);
    }
}
