package svs.memcached.server;

/**
 * Exception decoding message
 * Indication of malformed input
 *
 * Created by ssmirnov on 2/5/17.
 */
public class DecodingException extends Exception {
    public DecodingException(Exception e) {
        super(e);
    }
}
