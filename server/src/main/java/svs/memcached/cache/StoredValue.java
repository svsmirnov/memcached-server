package svs.memcached.cache;

import java.util.Arrays;

/**
 * Wrapper to store byte data along with flags
 * We need flags to support client serialization
 *
 * Created by ssmirnov on 2/5/17.
 *
 */
public class StoredValue {
    final byte[] data;
    final int flags;
    final int targetTimeSec;

    public StoredValue(byte[] data, int flags, int targetTimeSec) {
        this.data = data;
        this.flags = flags;
        this.targetTimeSec = targetTimeSec;
    }

    public byte[] getData() {
        return data;
    }

    public int getFlags() {
        return flags;
    }

    public int getTargetTimeSec() {
        return targetTimeSec;
    }

    @Override
    public String toString() {
        return "StoredValue{" +
                "data=" + Arrays.toString(data) +
                ", flags=" + flags +
                ", targetTimeSec=" + targetTimeSec +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StoredValue that = (StoredValue) o;

        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
