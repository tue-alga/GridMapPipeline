package arrange.parameter;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class MissingArgumentException extends ArgumentException {

    MissingArgumentException(String type, String argument) {
        super("Error: missing " + type + " argument after '" + argument + "'");
    }
}
