package svs.memcached.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Memcached text command decoder
 *
 * It is based on ReplayingDecoder for simplicity. As the result there may be some performance overhead
 * especially for large messages (e.g. decoding parts of the message multiple times)
 *
 *
 * Created by ssmirnov on 2/4/17.
 */
public class MemcacheDecoder extends ReplayingDecoder<Void> {

    private static final Logger logger = LogManager.getLogger(MemcacheDecoder.class);
    private static final int SECONDS_IN_30_DAYS = 60 * 60 * 24 * 30;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        try {
            int length = in.bytesBefore((byte) ' ');
            final String command = readString(in, length);
            in.skipBytes(1);  // space
            switch (command) {
                case "get": {
                    decodeGetCommand(in, out);
                    break;
                }
                case "set": {
                    decodeSetCommand(in, out);
                    break;
                }
                default: {
                    throw new UnsupportedCommandException(command);
                }
            }
        } catch (Exception e){
            if (!(e instanceof UnsupportedCommandException)){
                throw new DecodingException(e);
            } else {
                throw e; // rethrowing UnsupportedCommandException
            }
        }

    }

    private void decodeGetCommand(ByteBuf in, List<Object> out) {
        logger.debug("Decoding Get command");
        int length = in.bytesBefore((byte) '\r');
        final String key = readString(in, length);
        in.skipBytes(2); // \r\n
        out.add(MemcacheInboundCommand.newGetCommand(key));
    }


    private void decodeSetCommand(ByteBuf in, List<Object> out) {
        logger.debug("Decoding Set command");
        int length;
        length = in.bytesBefore((byte) ' ');
        final String key = readString(in, length);
        in.skipBytes(1); // space
        length = in.bytesBefore((byte) ' ');
        int flags = Integer.parseInt(readString(in, length)); // read flags; we will need them later for deserialization
        in.skipBytes(1); // space
        length = in.bytesBefore((byte) ' ');
        int targetTime = targetTimeSec(Integer.parseInt(readString(in, length)));
        in.skipBytes(1); // space
        length = in.bytesBefore((byte) '\r');
        int dataSize = Integer.parseInt(readString(in, length));
        in.skipBytes(2); // \r\n
        byte[] data = new byte[dataSize];
        in.readBytes(data);
        in.skipBytes(2); // \r\n
        out.add(MemcacheInboundCommand.newSetCommand(key, data, flags, targetTime));
    }

//    <exptime> is expiration time. If it's 0, the item never expires
//            (although it may be deleted from the cache to make place for other
//    items). If it's non-zero (either Unix time or offset in seconds from
//    current time), it is guaranteed that clients will not be able to
//    retrieve this item after the expiration time arrives (measured by server time).
//    If a negative value is given the item is immediately expired.
//      The actual value sent may either be Unix time (number of seconds since January 1, 1970, as a 32-bit value),
//      or a number of seconds starting from current time. In the latter case, this number of seconds may not exceed 60*60*24*30
//      (number of seconds in 30 days); if the number sent by a client is larger than that, the server will consider
//      it to be real Unix time value rather than an offset from current time.
    private int targetTimeSec(int expTime) {
        return expTime < 1 ? expTime :
                expTime > SECONDS_IN_30_DAYS ? expTime :
                        (int)(System.currentTimeMillis() / 1000) + expTime;
    }

    private @Nonnull String readString(ByteBuf in, int length){
        byte[] dest = new byte[length];
        in.readBytes(dest);
        return new String(dest, Charset.defaultCharset());
    }

}
