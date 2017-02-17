package svs.memcached.server;

import svs.memcached.cache.StoredValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * Memcache Inbound Command
 * It is decoded memcache command
 *
 * Created by ssmirnov on 2/4/17.
 */
public class MemcacheInboundCommand {

    private final String key;
    private final StoredValue value;
    private final CommandType type;


    private MemcacheInboundCommand(@Nonnull String key, @Nullable StoredValue value, @Nonnull CommandType type){
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public static MemcacheInboundCommand newSetCommand(String key, byte[] data, int flags, int targetTimeSec) {
        return new MemcacheInboundCommand(key, new StoredValue(data, flags, targetTimeSec), CommandType.SET);
    }

    public static MemcacheInboundCommand newGetCommand(String key) {
        return new MemcacheInboundCommand(key, null, CommandType.GET);
    }

    public String getKey() {
        return key;
    }

    public StoredValue getValue() {
        return value;
    }

    public CommandType getType() {
        return type;
    }


    @Override
    public String toString() {
        return "MemcacheInboundCommand{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
