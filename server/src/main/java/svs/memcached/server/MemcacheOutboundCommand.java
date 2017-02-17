package svs.memcached.server;

import svs.memcached.cache.StoredValue;

import javax.annotation.Nullable;

/**
 * Memcache Outbound Command
 * Represents the result of processed MemcacheInboundCommand
 *
 * Created by ssmirnov on 2/4/17.
 */
public class MemcacheOutboundCommand {

    private final CommandType type;
    private final String key;
    private final StoredValue value;

    private MemcacheOutboundCommand(@Nullable CommandType type, @Nullable String key, @Nullable StoredValue value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /**
     * Creates new Outbound Response Command for successful Get operation
     * @param command inbound Get command
     * @param value value obtained from the cache
     * @return constructed MemcacheOutboundCommand
     */
    public static MemcacheOutboundCommand newGetCommandResult(MemcacheInboundCommand command, StoredValue value) {
        return new MemcacheOutboundCommand(CommandType.GET, command.getKey(), value);
    }

    /**
     * Creates new Outbound Response Command for successful Set operation
     * @return constructed MemcacheOutboundCommand
     */
    public static MemcacheOutboundCommand newSetCommandResult() {
        return new MemcacheOutboundCommand(CommandType.SET, null, null);
    }

    public CommandType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public StoredValue getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "MemcacheOutboundCommand{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
