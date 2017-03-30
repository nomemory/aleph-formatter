package net.andreinc.aleph;

import static net.andreinc.aleph.AlephFormatter.template;

/**
 * Created by andreinicolinciobanu on 30/03/17.
 */
public class UncheckedFormatterException extends RuntimeException {

    public UncheckedFormatterException() {
        super();
    }
    public UncheckedFormatterException(String message) {
        super(message);
    }
    public UncheckedFormatterException(String message, Throwable cause) {
        super(message, cause);
    }
    public UncheckedFormatterException(Throwable cause) { super(cause);}

    private static final String INVALID_NUMBER_OF_ARGUMENTS =
            "Invalid number of arguments: {argsNum}. Every argument needs to have a pair.";

    private static final String ARGUMENT_ALREADY_DEFINED =
            "Argument '#{arg}' is already defined in the arguments list.";

    private static final String INVALID_CHARACTER_IN_PARAM_NAME =
            "Invalid character '#{char}' used in param name (affected index: #{idx}).";

    public static UncheckedFormatterException invalidNumberOfArguments(int argsNum) {
        String msg = template(INVALID_NUMBER_OF_ARGUMENTS).arg("argsNum", argsNum).fmt();
        return new UncheckedFormatterException(msg);
    }

    public static UncheckedFormatterException argumentAlreadyExist(String arg) {
        String msg = template(ARGUMENT_ALREADY_DEFINED).arg("arg", arg).fmt();
        return new UncheckedFormatterException(msg);
    }

    public static UncheckedFormatterException invalidCharacterInParam(char c, int idx) {
        String msg = template(INVALID_CHARACTER_IN_PARAM_NAME).args("char", c, "idx", idx).fmt();
        return new UncheckedFormatterException(msg);
    }
}
