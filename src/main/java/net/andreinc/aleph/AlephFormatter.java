package net.andreinc.aleph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.nio.file.Files.readAllBytes;
import static java.util.Arrays.asList;
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

    private List<Object> posArguments = new ArrayList<>();

    private AlephFormatter(String str) {
        this.str = str;
    }

    public static AlephFormatter str(String str) {
        return new AlephFormatter(str);
    }

    public static AlephFormatter str(String str, Object... args) {
        AlephFormatter af = new AlephFormatter(str);
        if (args!=null) {
            af.posArguments = asList(args);
        }
        return af;
    }

    public static AlephFormatter str(String str, Map<String, Object> args) {
        return str(str).args(args);
    }

    public static AlephFormatter file(String strPath) { return str(readFromFile(strPath)); }

    public static AlephFormatter file(String strPath, Charset encoding) { return str(readFromFile(strPath, encoding)); }

    public static AlephFormatter file(String strPath, Object... args) { return str(readFromFile(strPath), args); }

    public static AlephFormatter file(String strPath, Charset encoding, Object... args) { return str(readFromFile(strPath, encoding), args); }

    public static AlephFormatter file(String strPath, Map<String, Object> args) { return str(readFromFile(strPath), args); }

    public static AlephFormatter file(String strPath, Charset encoding, Map<String, Object> args) { return str(readFromFile(strPath, encoding), args); }

    public void failIfArgExists(String argName) {
        if (arguments.containsKey(argName))
            throw argumentAlreadyExist(argName);
    }

    public static String readFromFile(String strPath, Charset encoding) {
        try {
            byte[] encodedBytes = readAllBytes(Paths.get(strPath));
            return new String(encodedBytes, encoding);
        } catch (IOException e) {
            throw ioExceptionReadingFromFile(strPath, e);
        }
    }

    public static String readFromFile(String strPath) {
        return readFromFile(strPath, Charset.forName("UTF8"));
    }

    public AlephFormatter arg(String argName, Object object) {
        failIfArgExists(argName);
        this.arguments.put(argName, object);
        return this;
    }

    public AlephFormatter args(Map<String, Object> args) {
        for(Map.Entry<String, Object> entry : args.entrySet()) {
            failIfArgExists(entry.getKey());
            this.arguments.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public AlephFormatter args(Object... args) {

        if (args.length % 2 == 1)
            throw invalidNumberOfArguments(args.length);

        String key;

        for (int i = 0; i < args.length; i+=2) {
            key = (String) args[i];
            failIfArgExists(key);
            this.arguments.put(key, args[i+1]);
        }

        return this;
    }

    /**
     */
    public String fmt() {

        final StringBuilder result =  new StringBuilder(str.length());
        final StringBuilder param = new StringBuilder(16);

        State state = FREE_TEXT;

        int i = 0;
        char chr;
        while(i < str.length()) {
            chr = str.charAt(i);
            state = nextState(state, i);
            switch (state) {
                // In this state we just add the character to the
                // resulting buffer. No need to perform any processing.
                case FREE_TEXT : { result.append(chr); break; }
                // We identify '#'. We skip the following '{'.
                case PARAM_START:  { i++; break; }
                // We append the character to the param chain buffer
                case PARAM: { validateParamChar(chr, i); param.append(chr); break; }
                // We append and replace the param with the correct value
                case PARAM_END: { appendParamValue(param, result); break; }
                // Escape character
                case ESCAPE_CHAR: break;
            }
            i++;
        }

        return result.toString();
    }

    // The method that is used to change the states depending on the index
    // in the str and the current value of the character
    private State nextState(State currentState, int i) {
        switch (currentState) {
            case FREE_TEXT      : return jumpFromFreeText(str, i);
            case PARAM_START    : return jumpFromParamStart(str, i);
            case PARAM          : return jumpFromParam(str, i);
            case PARAM_END      : return jumpFromParamEnd(str, i);
            case ESCAPE_CHAR    : return FREE_TEXT;
            // Should never go here
            default             : throw invalidStateException(currentState);
        }
    }


    private static State jumpFromFreeText(String fmt, int idx) {
        if (isEscapeChar(fmt, idx))
            return ESCAPE_CHAR;
        if (isParamStart(fmt, idx))
            return PARAM_START;
        return FREE_TEXT;
    }

    private static State jumpFromParamStart(String fmt, int idx) {
        if (isParamEnd(fmt, idx))
            return PARAM_END;
        return PARAM;
    }

    private static State jumpFromParam(String fmt, int idx) {
        if (isParamEnd(fmt, idx))
            return PARAM_END;
        return PARAM;
    }

    private static State jumpFromParamEnd(String fmt, int idx) {
        if (isEscapeChar(fmt, idx))
            return ESCAPE_CHAR;
        if (isParamStart(fmt, idx))
            return PARAM_START;
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

    // This methods gets called when we want to obtain the value of the parameter
    //
    // - The parameter can be a simple argument "#{intVal}" and in this case
    // it is obtained directly from the arguments map.
    //
    // - The parameter can be a method chain argument: "#{address.getLine1.getNumber}"
    // in this case it is obtained by calling recursively the methods on the last obtained object
    private void appendParamValue(StringBuilder param, StringBuilder result) {

        if (param == null)
            throw invalidArgumentName(param);

        // Object name is the parameter that should be found in the map.
        // If it's followed by points, the points remain in the "param" buffer.
        final String objectName = takeUntilDotOrEnd(param);

        // Checks if object is positional parameter or named parameter
        // Positional parameters are always numbers
        Object objectValue;
        try {
            Integer objectIndex = Integer.parseInt(objectName);
            try {
                objectValue = posArguments.get(objectIndex);
            } catch (IndexOutOfBoundsException e) {
                throw invalidPositionalArgumentValue(objectIndex);
            }
        } catch(NumberFormatException nex) {
            // The parameter is not a positional argument
            objectValue = arguments.get(objectName);
        }

        Object toAppend;

        if (param.length() != 0) {
            // If this is a chain object.method1.method2.method3
            // we recurse
            toAppend = valueInChain(objectValue, param);
        } else {
            // We evaluate if the obejct is an array
            // If it's an array we print it nicely
            toAppend = evaluateIfArray(objectValue);
        }

        result.append(toAppend);
    }

    private static Object evaluateIfArray(Object o) {
        if (null != o && o.getClass().isArray())
            return arrayToString(o);
        return o;
    }

    private static String arrayToString(Object array) {
        final StringBuilder buff = new StringBuilder("[");

        for(int i = 0; i < getLength(array); ++i)
            buff.append(get(array, i)).append(", ");

        return clearLastComma(buff).append("]").toString();
    }

    private static StringBuilder clearLastComma(StringBuilder buff) {
        int lastComma = buff.lastIndexOf(", ");

        // No comma found, take everything
        if (-1 != lastComma)
            buff.delete(lastComma, buff.length());

        return buff;
    }

    // This method takes the section until the end of the buff or
    // until it finds the first dot ".".
    private static String takeUntilDotOrEnd(StringBuilder buff) {

        final int firstPointIdx = buff.indexOf(".");
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

        final String methodName = takeUntilDotOrEnd(paramBuffer);

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
                final String capital = methodName.substring(0, 1).toUpperCase();
                final String nameCapitalized = "get" + capital + methodName.substring(1);
                method = object.getClass().getMethod(nameCapitalized);
            } catch (NoSuchMethodException e1) {
                return null;
            }
        }

        return method;
    }
}
