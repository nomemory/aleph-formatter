package net.andreinc.aleph;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static net.andreinc.aleph.AlephFormatter.State.*;

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
        PARAM_END
    }

    private final String str;
    private final Map<String, Object> args = new HashMap<>();

    private AlephFormatter(String str) {
        this.str = str;
    }

    public static AlephFormatter template(String str) {
        return new AlephFormatter(str);
    }

    public AlephFormatter arg(String argName, Object object) {
        this.args.put(argName, object);
        return this;
    }

    public AlephFormatter args(Map<String, Object> args) {
        for(Map.Entry<String, Object> entry : args.entrySet()) {
            this.args.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public AlephFormatter args(Object... args) {
        if (args.length % 2 == 1) {
            throw new IllegalArgumentException("Invalid number of parameters. Every key needs to mach a value.");
        }

        for (int i = 0; i < args.length; i+=2) {
            String key = (String) args[i];
            Object value = args[i+1];
            this.args.put(key, value);
        }

        return this;
    }

    /**
     */
    public String format() {

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
                case PARAM: param.append(chr); break;

                // We append and replace the param with the correct value
                case PARAM_END: appendParamValue(param, result); break;
            }
            i++;
        }

        return result.toString();
    }

    // The method that is used to change the states depending on the index
    // in the string.
    private State nextState(State currentState, int i) {
        switch (currentState) {
            case FREE_TEXT      : return evaluateFreeText(str, i);
            case PARAM_START    : return evaluateParamStart(str, i);
            case PARAM          : return evaluateParam(str, i);
            case PARAM_END      : return evaluateParamEnd(str, i);
            default             : throw new IllegalArgumentException("Invalid state exception when parsing string.");
        }
    }

    // This methods gets called when we want to obtain the value of the parameter
    //
    // - The parameter can be a simple argument "#{intVal}" and in this case
    // it is obtained directly from the args map.
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
        Object objectValue = args.get(objectName);


        result.append(
                (param.length() != 0) ?
                        valueInChain(objectValue, param) :
                        objectValue);
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
            return object;
        }

        String methodName = takeUntilDotOrEnd(paramBuffer);

        try {
            Method method = object.getClass().getMethod(methodName);
            Object newObject = method.invoke(object);
            return valueInChain(newObject, paramBuffer);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return null;
        }
    }


    private static State evaluateFreeText(String fmt, int idx) {
        if (isParamStart(fmt, idx))
            return PARAM_START;
        return FREE_TEXT;
    }

    private static State evaluateParamStart(String fmt, int idx) {
        if (isParamEnd(fmt, idx))
            return PARAM_END;
        return PARAM;
    }

    private static State evaluateParam(String fmt, int idx) {
        if (isParamEnd(fmt, idx))
            return PARAM_END;
        return PARAM;
    }

    private static State evaluateParamEnd(String fmt, int idx) {
        if (isParamStart(fmt, idx))
            return PARAM_START;
        return FREE_TEXT;
    }

    private static boolean isParamStart(String fmt, int idx) {
        return ('#' == fmt.charAt(idx)) && (idx+1< fmt.length() &&  ('{' == fmt.charAt(idx+1)));
    }

    private static boolean isParamEnd(String fmt, int idx) {
        return '}' == fmt.charAt(idx);
    }

}
