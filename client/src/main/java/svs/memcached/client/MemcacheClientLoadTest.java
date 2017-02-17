package svs.memcached.client;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Load test for memcached server
 *
 * It is based on spymemcached client
 * It uses several threads each of them are repeatedly executing get and set operations against local memcached server
 *
 * Please note it is not pure performance measurement test as it has additional overhead associated with initializing
 * and shutting down clients and results verification
 *
 * Created by ssmirnov on 2/5/17.
 */
public class MemcacheClientLoadTest implements Runnable {

    private static final Logger logger = LogManager.getLogger(MemcacheClientLoadTest.class);

    private static final int NUMBER_OF_THREADS = 5; // number of threads to run in parallel
    private static final int LOOP_ITERATIONS_PER_THREAD = 50000; // each loop iteration calls 2 async sets and 2 asyncGet-s
    // all TEST_REPETITIONS are executed by the pool of NUMBER_OF_THREADS threads
    private static final int TEST_REPETITIONS = 20; // number of times to repeat LOOP_ITERATIONS_PER_THREAD
    private static final String DEFAULT_SERVER_ADDRESSES = "127.0.0.1:11211";

    public static final int NORMAL_MEMCACHED_CLIENT_OP_TIMEOUT = 3000;
    // increase timeout so we don't throw exceptions under high load
    public static final int LOAD_MEMCACHED_CLIENT_OP_TIMEOUT = 60000;
    // How long to wait until test completes
    public static final int EXECUTOR_TERMINATION_TIMEOUT_SEC = 3600; // 1 hr

    private static List<InetSocketAddress> serverAddressList; // list of server addresses to use during tests

    private static final AtomicLong totalCommands = new AtomicLong(0);
    private static final AtomicLong sequence = new AtomicLong(0);
    private static long globalStartTime;

    private final MemcachedClient memcacheClient;
    private final Random random; // random generator to generate test values
    private final int taskNumber;


    public MemcacheClientLoadTest(List<InetSocketAddress> serverAddressList, int taskNumber) throws IOException {
        this.memcacheClient = createMemcachedClient(serverAddressList, LOAD_MEMCACHED_CLIENT_OP_TIMEOUT);
        random = new Random();
        this.taskNumber = taskNumber;
    }

    public static void main(String[] args) throws Exception {

        final String addresses;
        if (args.length > 0) {
            addresses = args[0].trim();
            logger.info("Custom connect string: {}", addresses);
        } else {
            addresses = DEFAULT_SERVER_ADDRESSES;
        }

        serverAddressList = AddrUtil.getAddresses(addresses);
        logger.info("Will run tests for the following servers: {}", serverAddressList);

        runAllTests();

    }

    private static void runAllTests() throws IOException, InterruptedException, ExecutionException {
        initialTest(); // just test that server works before running load test

        // run a bunch of parallel tests
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        globalStartTime = System.currentTimeMillis();
        for (int i = 1; i <= TEST_REPETITIONS; i++) {
            logger.info("Submitting task {}", i);
            MemcacheClientLoadTest testTask = new MemcacheClientLoadTest(serverAddressList, i);
            executor.submit(testTask);
        }
        executor.shutdown();
        if (!executor.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            logger.error("!!! Executor termination timeout of {} minutes exceeded", EXECUTOR_TERMINATION_TIMEOUT_SEC);
            executor.shutdownNow();
        }

        long duration = System.currentTimeMillis() - globalStartTime;
        logger.info("<<<*** Load Test Finished Successfully ***>>>");
        logger.info("<<<*** Generated total: {} key-values ***>>>", sequence.get());
        logger.info("<<<*** Grand Total for All {} Threads: Commands Processed = {}; Time taken = {} ms ***>>>",
                NUMBER_OF_THREADS, totalCommands.get(), duration);
        // make sure our math is correct: we issue 1 set and 1 get command for every generated key
        Assert.assertEquals(sequence.get(), totalCommands.get() / 2);
        logger.info("<<<*** Average Ratio: {} commands per second ***>>>",
                (long)((double)totalCommands.get() / duration * 1000)); // lets be precise here
    }

    /**
     * Run few basic operation to ensure memcached server is up
     * @throws IOException
     */
    private static void initialTest() throws IOException, ExecutionException, InterruptedException {
        MemcachedClient memcacheClient = createMemcachedClient(serverAddressList, NORMAL_MEMCACHED_CLIENT_OP_TIMEOUT);
        try {
            logger.info("Running basic test...");
            // use time aligned key suffix to support multiple tests in parallel
            String keySuffix = Long.toString(System.currentTimeMillis());
            String objectToCache = "Test String";
            memcacheClient.set("keyName" + keySuffix, 3600, objectToCache);
            Date startDate = new Date();
            memcacheClient.set("keyDate" + keySuffix, 3600, startDate);
            String fetchedObject = (String) memcacheClient.get("keyName" + keySuffix);
            logger.info("Before: \"{}\"; After: \"{}\"", objectToCache, fetchedObject);
            assertEquals(objectToCache, fetchedObject);
            Date fetchedDate = (Date) memcacheClient.get("keyDate" + keySuffix);
            logger.info("Before: \"{}\"; After: \"{}\"", startDate, fetchedDate);
            assertEquals(startDate, fetchedDate);
            Object nonExistingValue = memcacheClient.get(UUID.randomUUID().toString());
            assertNull(nonExistingValue);
            String sharedKey = "expiredKey" + keySuffix;
            String sharedValue = "test";
            // test expiration
            Future<Boolean> future = memcacheClient.set(sharedKey, 1, sharedValue);
            assertTrue(future.get());
            Thread.sleep(1001);
            assertNull(memcacheClient.get(sharedKey));
            // test non expire value
            future = memcacheClient.set(sharedKey, 0, sharedValue); // never expire
            assertTrue(future.get());
            Thread.sleep(1001);
            assertEquals(sharedValue, memcacheClient.get(sharedKey));
            // test negative expiration - expire immediately
            future = memcacheClient.set(sharedKey, -1, sharedValue); // expire immediately
            assertTrue(future.get());
            assertNull(memcacheClient.get(sharedKey));

            logger.info("Basic test done - OK");
        } finally {
            memcacheClient.shutdown();
        }
    }

    private static MemcachedClient createMemcachedClient(List<InetSocketAddress> addressList, int operationTimeout)
            throws IOException {
        return new MemcachedClient(
                new ConnectionFactoryBuilder()
                        .setOpTimeout(operationTimeout).build(),
                addressList
        );
    }

    @Override
    @SuppressWarnings("unchecked") // asyncGet returns an Object type if Transcoder is not specified
    public void run() {
        logger.info("Starting task {}", taskNumber);
        try {
            final Map<String, VerificationPair<String>> strResultMap = new HashMap<>();
            final Map<String, VerificationPair<Double>> doubleResultMap = new HashMap<>();
            int numberOfCommands = LOOP_ITERATIONS_PER_THREAD * 4; // 2 sets and 2 gets per iteration
            logger.info("Starting executing memcached commands. Number of commands to execute: {}", numberOfCommands);
            long startTime = System.currentTimeMillis();
            // it will execute 2 memcached set (async) and 2 asyncGet operations per iteration
            for (int i = 0; i < LOOP_ITERATIONS_PER_THREAD; i++) {
                String strKey = nextKey();
                String strValue = strKey + "_value";
                String doubleKey = nextKey();
                double doubleValue = random.nextDouble();
                memcacheClient.set(strKey, 3600, strValue);
                memcacheClient.set(doubleKey, 3600, doubleValue);
                GetFuture strFuture = memcacheClient.asyncGet(strKey);
                strResultMap.put(strKey, new VerificationPair<>(strValue, strFuture));
                GetFuture doubleFuture = memcacheClient.asyncGet(doubleKey);
                doubleResultMap.put(doubleKey, new VerificationPair<>(doubleValue, doubleFuture));
            }
            long execEndTime = System.currentTimeMillis();
            logger.info("Done executing memcached commands");
            logger.info("Starting verification");
            // verify string values
            logger.info("\tStarting String values verification");
            for (VerificationPair pair: strResultMap.values()){
                pair.verify();
            }
            logger.info("\tDone String values verification");
            // verify double values
            logger.info("\tStarting Double values verification");
            for (VerificationPair pair: doubleResultMap.values()){
                pair.verify();
            }
            long totalEndTime = System.currentTimeMillis();
            logger.info("\tDone Double values verification");
            logger.info("Done verification");
            logger.info("Task {} :: Processed {} commands. Execution time={} ms; Verification Time={} ms; Total time = {} ms",
                    taskNumber, numberOfCommands, (execEndTime - startTime), (totalEndTime - execEndTime), (totalEndTime - startTime));
            totalCommands.addAndGet(numberOfCommands);
            logger.info("*** Running Total for All {} Threads: Commands Processed = {}; Time taken = {} ms ***",
                    NUMBER_OF_THREADS, totalCommands.get(), (totalEndTime - globalStartTime));
        } catch (Exception | AssertionError e) {
            logger.error(e);
            System.exit(13);
        } finally {
            memcacheClient.shutdown();
            logger.info("Finished task {}", taskNumber);
        }
    }

    /**
     * Creates pseudo-unique String key fast.
     * To avoid collisions between several test runs test start time is added
     * @return String representation of unique number within test run
     */
    private String nextKey() {
        // use 1,000,000 multiplier to support running multiple tests in parallel
        // it will minimize the chance of collision
        // basically if tests started 1 second apart there should be 1,000,000,000 keys generated to cause a collision
        return Long.toString(globalStartTime * 1000000 + sequence.incrementAndGet());
    }

    /**
     * Internal class that hold original value and future of corresponding get operation to verify stored value
     * @param <T> value type
     */
    private static class VerificationPair<T> {
        private final T originalValue;
        private final GetFuture<T> receivedValueFuture;

        public VerificationPair(T originalValue, GetFuture<T> receivedValueFuture) {
            this.originalValue = originalValue;
            this.receivedValueFuture = receivedValueFuture;
        }

        public void verify() throws ExecutionException, InterruptedException {
            Assert.assertEquals(originalValue, receivedValueFuture.get());
        }

    }


}
