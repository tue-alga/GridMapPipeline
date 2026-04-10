package arrange.parameter;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class ArgumentTypeException extends ArgumentException {

    ArgumentTypeException(String type, String argument) {
        super("Error: " + type + " argument expected after '" + argument + "'");
    }
}
