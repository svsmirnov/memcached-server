package svs.memcached.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import svs.memcached.cache.ICache;
import svs.memcached.cache.LocalCache;
import svs.memcached.cache.StoredValue;

/**
 *
 * Netty based Memcached server
 *
 * Netty 4 has support for memcache protocol, unfortunately it only supports binary
 * So implementing a simple prototype here
 *
 * Created by ssmirnov on 2/4/17.
 *
 *
 */
public class MemcachedServer {

    private static final Logger logger = LogManager.getLogger(MemcachedServer.class);

    private static final int DEFAULT_PORT = 11211;

    // cache params
    private static final int CACHE_MAX_SIZE = 1000000;
    private static final int CACHE_MAX_IDLE_TIME_MS = 600000; // 10 min max idle
    private static final int CACHE_CONCURRENCY_LEVEL = 32; // set higher concurrency level (default is 4)
    private static final boolean CACHE_EXPIRE_ON_MEMORY_PRESSURE = true;

    private static final int WORKER_THREADS_COUNT = Runtime.getRuntime().availableProcessors() * 2;
    private static final int SO_BACKLOG_VALUE = 128;

    private final int port;

    public MemcachedServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        final int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = DEFAULT_PORT;
        }
        logger.info("Starting server on port {}", port);
        MemcachedServer server = new MemcachedServer(port);
        try {
            server.bootstrapAndWait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void bootstrapAndWait() throws InterruptedException {
        logger.info("Bootstrapping Memcached Server");
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // business logic thread pool
        // It is probably overkill to use separate thread pool for cache access here but
        // generally it is good practice to separate IO from business logic
        final EventExecutorGroup mainGroup = new DefaultEventExecutorGroup(WORKER_THREADS_COUNT);
        try {
            final ICache<StoredValue> cache = new LocalCache(CACHE_MAX_SIZE, CACHE_MAX_IDLE_TIME_MS,
                    CACHE_CONCURRENCY_LEVEL, CACHE_EXPIRE_ON_MEMORY_PRESSURE);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);

            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new MemcacheDecoder());
                    pipeline.addLast(new MemcacheEncoder());
                    // Cache Operations Command Handler
                    pipeline.addLast(mainGroup, "commandHandler", new MemcacheCommandHandler(cache));
                }
            });

            bootstrap.option(ChannelOption.SO_BACKLOG, SO_BACKLOG_VALUE);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(port).sync();
            logger.info("Server is ready <port={}>...", port);
            f.channel().closeFuture().sync();
        } finally {
            logger.info("Shutting down server...");
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
