package svs.memcached.server;

/**
 * Indicates that protocol command is not supported
 *
 * Created by ssmirnov on 2/5/17.
 */
public class UnsupportedCommandException extends Exception {

    public UnsupportedCommandException(String command) {
        super("Command is not supported: " + command);
    }
}
