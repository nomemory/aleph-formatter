package net.andreinc.aleph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.nio.file.Files.readAllBytes;
import static net.andreinc.aleph.AlephFormatter.State.*;
import static net.andreinc.aleph.UncheckedFormatterException.*;

public class AlephFormatter {

    //
    // Given we have a formatting String:
    //
    //      "Address: #{address.line1}, #{address.line2}, #{someNumber}
    //
    // Each corresponds to a certain index position of the following
    // String.
    //
    protected enum State {
        // Corresponds to the state when we are not inside the curly braces:
        //
        //      <'here'> #{} <'here'>
        //
        FREE_TEXT,

        // Corresponds to the state when we are inside the curly braces:
        //
        //      #{'<here>'}
        //
        PARAM,

        // Corresponds to the state when we are visiting the characters '#{'
        PARAM_START,

        // Corresponds to the state when we are visiting the character '}'
        PARAM_END,

        // Escape char
        ESCAPE_CHAR
    }

    private final String str;

    private final Map<String, Object> arguments = new HashMap<>();

    private AlephFormatter(String str) {
        this.str = str;
    }

    public static AlephFormatter template(String str) {
        return new AlephFormatter(str);
    }

    public static AlephFormatter template(String str, Object... args) {
        return template(str).args(args);
    }

    public static AlephFormatter template(String str, Map<String, Object> args) {
        return template(str).args(args);
    }

    public static AlephFormatter fromFile(String strPath) { return template(readFromFile(strPath)); }

    public static AlephFormatter fromFile(String strPath, Charset encoding) { return template(readFromFile(strPath, encoding)); }

    public static AlephFormatter fromFile(String strPath, Object... args) { return template(readFromFile(strPath), args); }

    public static AlephFormatter fromFile(String strPath, Charset encoding, Object... args) { return template(readFromFile(strPath, encoding), args); }

    public static AlephFormatter fromFile(String strPath, Map<String, Object> args) { return template(readFromFile(strPath), args); }

    public static AlephFormatter fromFile(String strPath, Charset encoding, Map<String, Object> args) { return template(readFromFile(strPath, encoding), args); }

    public static String readFromFile(String strPath, Charset encoding) {
        try {
            byte[] encodedBytes = readAllBytes(Paths.get(strPath));
            return new String(encodedBytes, encoding);
        } catch (IOException e) {
            throw ioExceptionReadingFromFile(strPath, e);
        }
    }

    public static String readFromFile(String strPath) {
        //TODO validate strPath
        return readFromFile(strPath, Charset.forName("UTF8"));
    }

    public AlephFormatter arg(String argName, Object object) {
        this.arguments.put(argName, object);
        return this;
    }

    public AlephFormatter args(Map<String, Object> args) {
        for(Map.Entry<String, Object> entry : args.entrySet()) {
            if (this.arguments.containsKey(entry.getKey()))
                throw argumentAlreadyExist(entry.getKey());
            this.arguments.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public AlephFormatter args(Object... args) {

        if (args.length % 2 == 1)
            throw invalidNumberOfArguments(args.length);

        for (int i = 0; i < args.length; i+=2) {
            String key = (String) args[i];

            // If the argument exists throw an error
            if (this.arguments.containsKey(key))
                throw argumentAlreadyExist(key);

            Object value = args[i+1];
            this.arguments.put(key, value);
        }

        return this;
    }

    /**
     */
    public String fmt() {

        StringBuilder result =  new StringBuilder(str.length());
        StringBuilder param = new StringBuilder(16);

        State state = FREE_TEXT;

        int i = 0;
        while(i < str.length()) {
            char chr = str.charAt(i);
            state = nextState(state, i);
            switch (state) {
                // In this state we just add the character to the
                // resulting buffer. No need to perform any processing.
                case FREE_TEXT : result.append(chr); break;

                // We identify '#'. We skip the following '{'.
                case PARAM_START: i++; break;

                // We append the character to the param chain buffer
                case PARAM: { validateParamChar(chr, i); param.append(chr); break; }

                // We append and replace the param with the correct value
                case PARAM_END: appendParamValue(param, result); break;

                // Escape character
                case ESCAPE_CHAR: break;
            }
            i++;
        }

        return result.toString();
    }

    // The method that is used to change the states depending on the index
    // in the string and the current value of the character
    private State nextState(State currentState, int i) {
        switch (currentState) {
            case FREE_TEXT      : return jumpFromFreText(str, i);
            case PARAM_START    : return jumpFromParamStart(str, i);
            case PARAM          : return jumpFromParam(str, i);
            case PARAM_END      : return jumpFromParamEnd(str, i);
            case ESCAPE_CHAR    : return FREE_TEXT;
            default             : throw new IllegalArgumentException("Invalid state exception when parsing string.");
        }
    }

    // This methods gets called when we want to obtain the value of the parameter
    //
    // - The parameter can be a simple argument "#{intVal}" and in this case
    // it is obtained directly from the arguments map.
    //
    // - The parameter can be a method chain argument: "#{address.getLine1.getNumber}"
    // in this case it is obtained by calling recursively the methods on the last obtained object
    private void appendParamValue(StringBuilder param, StringBuilder result) {

        if (param == null) {
            throw new IllegalArgumentException("Invalid parameter name to append (NULL).");
        }

        // Object name is the parameter that should be found in the map.
        // If it's followed by points, the points remain in the "param" buffer.
        String objectName = takeUntilDotOrEnd(param);
        Object objectValue = arguments.get(objectName);


        result.append(
                (param.length() != 0) ?
                        valueInChain(objectValue, param) :
                        evaluateIfArray(objectValue));
    }

    private static Object evaluateIfArray(Object o) {
        if (null != o && o.getClass().isArray())
            return arrayToString(o);
        return o;
    }

    private static String arrayToString(Object array) {
        StringBuilder buff = new StringBuilder("[");

        for(int i = 0; i < getLength(array); ++i)
            buff.append(get(array, i)).append(", ");

        return clearLastComma(buff).append("]").toString();
    }

    private static StringBuilder clearLastComma(StringBuilder buff) {
        int lastComma = buff.lastIndexOf(", ");
        if (-1 != lastComma)
            buff.delete(lastComma, buff.length());
        return buff;
    }

    // This method takes the section until the end of the buff or
    // until it finds the first dot ".".
    private static String takeUntilDotOrEnd(StringBuilder buff) {

        int firstPointIdx = buff.indexOf(".");
        String result;

        if (-1 == firstPointIdx) {
            result = buff.toString();
            buff.setLength(0);
        } else {
            result = buff.substring(0, firstPointIdx);
            buff.delete(0, firstPointIdx + 1);
        }

        return result;
    }

    // Recursively obtain the value from the method chain by invoking the methods
    // using reflection on the last object obtained.
    private static Object valueInChain(Object object, StringBuilder paramBuffer) {

        // When last obtained is null or when there are no more methods in the chain
        // we stop
        if (object == null || paramBuffer.length() == 0) {
            return evaluateIfArray(object);
        }

        String methodName = takeUntilDotOrEnd(paramBuffer);

        Object newObject;
        try {
            // Try with the given method or with the getter as a fallback
            Method method = getMethodOrGetter(object, methodName);
            if (null == method)
                return null;
            newObject = method.invoke(object);
            return valueInChain(newObject, paramBuffer);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // Couldn't invoke the method
            return null;
        }
    }

    public static Method getMethodOrGetter(Object object, String methodName) {
        Method method;
        try {
            method = object.getClass().getMethod(methodName);
        }  catch (NoSuchMethodException e) {
            try {
                // Maybe improve this
                String capital = methodName.substring(0, 1).toUpperCase();
                String nameCapitalized = "get" + capital + methodName.substring(1);
                method = object.getClass().getMethod(nameCapitalized);
            } catch (NoSuchMethodException e1) {
                return null;
            }
        }
        return method;
    }

    private static State jumpFromFreText(String fmt, int idx) {
        if (isEscapeChar(fmt, idx)) return ESCAPE_CHAR;
        if (isParamStart(fmt, idx)) return PARAM_START;
        return FREE_TEXT;
    }

    private static State jumpFromParamStart(String fmt, int idx) {
        if (isParamEnd(fmt, idx)) return PARAM_END;
        return PARAM;
    }

    private static State jumpFromParam(String fmt, int idx) {
        if (isParamEnd(fmt, idx)) return PARAM_END;
        return PARAM;
    }

    private static State jumpFromParamEnd(String fmt, int idx) {
        if (isEscapeChar(fmt, idx)) return ESCAPE_CHAR;
        if (isParamStart(fmt, idx)) return PARAM_START;
        return FREE_TEXT;
    }

    private static boolean isParamStart(String fmt, int idx) {
        return ( '#' == fmt.charAt(idx) ) &&
               ( idx + 1 < fmt.length() &&  ( '{' == fmt.charAt(idx+1)) );
    }

    private static boolean isParamEnd(String fmt, int idx) {
        return '}' == fmt.charAt(idx);
    }

    private static boolean isEscapeChar(String fmt, int idx) {
        return '`' == fmt.charAt(idx);
    }

    private static void validateParamChar(char cc, int idx) {
        if ( !(isDigit(cc) || isLetter(cc) || '.'== cc) )
            throw invalidCharacterInParam(cc, idx);
    }
}
