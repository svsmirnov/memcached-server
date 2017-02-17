package svs.memcached.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import svs.memcached.cache.ICache;
import svs.memcached.cache.StoredValue;

/**
 *
 * Main Command handler
 * Performs actual cache operations
 *
 * Created by ssmirnov on 2/4/17.
 */
public class MemcacheCommandHandler extends SimpleChannelInboundHandler<MemcacheInboundCommand> {


    private final ICache<StoredValue> cache;

    private static final Logger logger = LogManager.getLogger(MemcacheCommandHandler.class);

    public MemcacheCommandHandler(ICache<StoredValue> cache) {
        this.cache = cache;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MemcacheInboundCommand command) throws Exception {
        logger.debug("Processing command: {}", command);

        switch (command.getType()) {
            case GET: {
                StoredValue value = cache.get(command.getKey());
                if (value != null && value.getTargetTimeSec() != 0
                        && value.getTargetTimeSec() * 1000l < System.currentTimeMillis()) {
                    value = null; // expired
                    cache.remove(command.getKey()); // TODO synchronization for concurrent access
                }
                ctx.writeAndFlush(MemcacheOutboundCommand.newGetCommandResult(command, value));
                break;
            }
            case SET: {
                if (command.getValue().getTargetTimeSec() < 0) { // If a negative value is given the item is immediately expired.
                    cache.remove(command.getKey());
                } else {
                    cache.set(command.getKey(), command.getValue());
                }
                ctx.writeAndFlush(MemcacheOutboundCommand.newSetCommandResult());
                break;
            }
            default : {
                throw new IllegalArgumentException("Unsupported command type: " + command.getType());
            }
        }
    }
}
