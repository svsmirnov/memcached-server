package svs.memcached.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * Guava based implementation of simple in-memory cache
 * Created by ssmirnov on 2/4/17.
 */
public class LocalCache implements ICache<StoredValue> {

    private static final Logger logger = LogManager.getLogger(LocalCache.class);
    private final Cache<String, StoredValue> cache;

    public LocalCache(int maxSize, long maxIdleTimeMs, int concurrencyLevel, boolean expireOnMemoryPressure) {
        logger.info("Initializing cache with maxSize={}, maxIdleTimeMs={}, concurrencyLevel={}, expireOnMemoryPressure={}",
                maxSize, maxIdleTimeMs, concurrencyLevel, expireOnMemoryPressure);
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.maximumSize(maxSize).
                expireAfterAccess(maxIdleTimeMs, TimeUnit.MILLISECONDS).
                concurrencyLevel(concurrencyLevel);
        // Soft values can save from OOMs but generally not recommended because
        // they can have additional performance impact.
        // However if average value size is unknown and may vary a lot soft values can be very helpful
        if (expireOnMemoryPressure) {
            builder.softValues();
        }
        cache = builder.<String, StoredValue>build();
    }

    @Override
    public void set(@Nonnull String key, @Nonnull StoredValue value) {
        logger.debug("Put:: Key={}, Value={}", key, value);
        cache.put(key, value);
    }

    @Nullable
    @Override
    public StoredValue get(@Nonnull String key) {
        final StoredValue result = cache.getIfPresent(key);
        logger.debug("Get:: for Key={} returned Value={}", key, result);
        return result;
    }

    @Override
    public void remove(@Nonnull String key) {
        cache.invalidate(key);
        logger.debug("Remove:: Key={}", key);
    }

}
