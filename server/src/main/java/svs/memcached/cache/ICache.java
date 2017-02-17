package svs.memcached.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Simple Cache Interface
 * Created by ssmirnov on 2/4/17.
 */
public interface ICache<T> {
    /**
     * Put entry in the cache
     * @param key - cache key
     * @param value - Value to put
     */
    void set(@Nonnull String key, @Nonnull T value);

    /**
     * Get entry from the cache
     * @param key cache key
     * @return stored value of Null if value not found
     */
    @Nullable
    T get(@Nonnull String key);


    void remove(@Nonnull String key);
}
