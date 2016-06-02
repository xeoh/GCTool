package edu.kaist.algo.api;

import com.google.common.annotations.VisibleForTesting;

import edu.kaist.algo.service.LogUploadGrpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;

/**
 * GcToolServer class that provides various services related to analyzing
 * Java Garbage Collector logs.
 */
public class GcToolServer {
  private static final String DEFAULT_REDIS_HOST = "localhost";
  private static final Logger logger = LoggerFactory.getLogger(GcToolServer.class);
  private final JedisPool jedisPool;
  private final int port;
  private final Server server;

  /**
   * Constructs a server at given PORT number.
   * Default JedisPool instance will be created.
   * @param port the port number.
   */
  public GcToolServer(int port) {
    this(port, new JedisPool(new JedisPoolConfig(), DEFAULT_REDIS_HOST));
  }

  /**
   * Constructor of GcToolServer having external dependency in jedisPool.
   * @param port port number
   * @param jedisPool JedisPool for redis server use
   */
  public GcToolServer(int port, JedisPool jedisPool) {
    this.port = port;
    this.jedisPool = jedisPool;
    this.server = ServerBuilder.forPort(port)
        .addService(LogUploadGrpc
            .bindService(new LogUploadImpl(new Ticketer(jedisPool))))
        .build();
  }

  @VisibleForTesting
  void start() throws IOException {
    server.start();
    logger.info("Server started, listening on " + this.port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** Shutting down gRPC server since JVM is shutting down");
        GcToolServer.this.stop();
        System.err.println("*** Server shut down");
      }
    });
  }

  private void stop() {
    jedisPool.destroy();
    if (server != null) {
      server.shutdown();
    }
  }

  // wait until the gRPC channel is severed
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  private static Options makeServerOptions() {
    Options options = new Options();
    Option port = Option.builder("p")
        .hasArg(true)
        .argName("port")
        .desc("give the port number")
        .build();

    options.addOption(port);
    return options;
  }

  private static int parseOptions(Options options, String[] args)
      throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    int port;
    try {
      port = Integer.parseInt(cmd.getOptionValue("p", "50051"));
    } catch (NumberFormatException nfe) {
      throw new ParseException("Port is not a number.");
    }

    if (port < 0 || port > 65535) {
      throw new ParseException("Invalid port value. (0 <= port <= 65535).");
    }

    return port;
  }

  /**
   * Opens a server that receives streams of file contents, which
   * is then summed and created into an uploaded GC logfile.
   * @param args command-line user input.
   */
  public static void main(String[] args) {
    final Options options = makeServerOptions();
    int port;
    try {
      port = parseOptions(options, args);
    } catch (ParseException pe) {
      logger.error("Parsing failed. Reason : " + pe.getMessage());
      return;
    }

    // start the server
    final GcToolServer serverInstance = new GcToolServer(port);
    try {
      serverInstance.start();
      serverInstance.blockUntilShutdown();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      logger.error("Server failed to initiate.", ioe);
      serverInstance.stop();
    } catch (InterruptedException ite) {
      ite.printStackTrace();
      logger.error("Thread interrupted.", ite);
      serverInstance.stop();
    }
  }
}
