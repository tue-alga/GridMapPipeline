package arrange.parameter;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class UnknownArgumentException extends ArgumentException {

    UnknownArgumentException(String argument) {
        super("Error: unknown argument '" + argument + "'");
    }
}
