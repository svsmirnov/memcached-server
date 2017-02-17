package svs.memcached.cache;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Basic tests for Cache
 *
 * Created by ssmirnov on 2/5/17.
 */
public class LocalCacheTest {

    ICache<StoredValue> cache;

    @Before
    public void initCache(){
        cache = new LocalCache(100, 1000, 4, true);
    }

    @Test
    public void testBasicOps(){
        StoredValue value = new StoredValue(new byte[3], 10, 100);
        cache.set("key", value);
        assertEquals(value, cache.get("key"));
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void testNullValueNotSupported() {
        cache.set("nullKey", null);
    }

    @Test
    public void testStoredValueEquality(){
        byte[] data1 = new byte[]{0,1,2};
        byte[] data2 = new byte[]{0,1,1};
        int flags1 = 0;
        int flags2 = 1;
        StoredValue value1 = new StoredValue(data1, flags1, 100);
        StoredValue value2 = new StoredValue(data1, flags2, 100);
        StoredValue value3 = new StoredValue(data2, flags1, 100);
        assertEquals(value1, value2);
        assertEquals(value1.hashCode(), value2.hashCode());
        assertNotEquals(value1, value3);
        assertNotEquals(value1.hashCode(), value3.hashCode());
        // let's test cache value overwrite as cache is already initialized
        cache.set("key1", value1);
        cache.set("key1", value3);
        assertNotEquals(value1, cache.get("key1"));
        assertEquals(value3, cache.get("key1"));
    }

}
