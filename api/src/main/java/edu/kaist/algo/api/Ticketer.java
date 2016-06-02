package edu.kaist.algo.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import edu.kaist.algo.service.AnalysisStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;

/**
 * The Ticketer class manages various information for GC log analysis.
 * It uses Jedis, a java redis connection library, to manage the data.
 * Avaliable resources are : analysis status, name of logfile, name of metadata file, and
 * name of analysis result file.
 *
 * <p>When the Garbage Collection log file is uploaded to server for analysis request,
 * the server will issue an identification called "ticket" for the client to identify
 * the specific log file stored within the server. The client can request various information
 * such as benchmark test time (stored in metadata file) or statistical analysis result
 * (stored in result file), based on the ticket number it has.
 */
public class Ticketer {
  private static final int DEFAULT_REDIS_PORT = 6379;
  private static final String LOCALHOST = "localhost";
  private static final String COUNTER = "counter";
  private static final String TICKET = "ticket";

  static final String STATUS = "status";
  static final String LOGFILE = "logfile";
  static final String META = "meta";
  static final String RESULT = "result";
  static final String META_NAME = "meta_name";
  static final String META_SIZE = "meta_size";

  private final JedisPool jedisPool;
  private final Logger logger = LoggerFactory.getLogger(Ticketer.class);

  /**
   * Creates a Ticketer instance.
   *
   * @param host jedis server host
   * @param port jedis port
   */
  public Ticketer(String host, int port) {
    this(new JedisPool(new JedisPoolConfig(), host, port));
  }

  /**
   * Creates a Ticketer instance with default port number 6379.
   *
   * @param host jedis server host
   */
  public Ticketer(String host) {
    this(host, DEFAULT_REDIS_PORT);
  }

  /**
   * Creates a Ticketer instance given JedisPool class instance.
   *
   * @param jedisPool JedisPool instance for jedis use in this class.
   */
  Ticketer(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  /**
   * Issues a ticket.
   * The ticket number is incremented by 1 whenever it is issued.
   *
   * @return ticket number
   */
  public long issueTicket() {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.incr(COUNTER);
    }
  }

  /**
   * Creates a 'key' string that is used for redis(jedis) value setting.
   *
   * <p>It has a format of ticket:TICKET_NUMBER:RESOURCE_NAME.
   * For example, if the ticket number is 2 and we want the result file name,
   * then the key will be : "ticket:2:result"
   *
   * <p>The resourceName argument SHOULD be either one of :
   * Ticketer.LOGFILE, Ticketer.STATUS, Ticketer.META, Ticketer.RESULT.
   *
   * @param ticketNum the ticket number
   * @param resourceName the string name of the resource
   * @return key string for jedis use
   */
  @VisibleForTesting
  static String makeKey(long ticketNum, String resourceName)
      throws IllegalArgumentException {

    // checks the number of ticket
    if (ticketNum <= 0) {
      throw new IllegalArgumentException("Invalid number : ticket number should be > 0");
    }

    // checks the validity of resource name
    if (!resourceName.equals(STATUS) && !resourceName.equals(LOGFILE)
        && !resourceName.equals(META) && !resourceName.equals(RESULT)) {
      throw new IllegalArgumentException("Invalid resource name.");
    }

    return new StringBuilder(TICKET).append(":")
        .append(ticketNum).append(":")
        .append(resourceName).toString();
  }

  /**
   * Gives the current status of GC analysis, given the ticket number.
   *
   * <p>The status is either NOT_READY, ERROR, COMPLETED, and ANALYZING.
   *
   * @param ticketNum the ticket number
   * @return enum status of current GC log analysis status
   */
  public AnalysisStatus getStatus(long ticketNum) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, STATUS);
      String statusString = jedis.get(key);
      if (statusString == null) {
        return null;
      }
      return AnalysisStatus.valueOf(statusString);
    }
  }

  /**
   * Sets the status of GC analysis.
   *
   * <p>The status is either NOT_READY, ERROR, COMPLETED, and ANALYZING.
   *
   * @param ticketNum the ticket number
   * @param status the enum Status to be set
   */
  public void setStatus(long ticketNum, AnalysisStatus status) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, STATUS);
      jedis.set(key, status.name());
    }
  }

  /**
   * Gives the name of the log file (original GC log data).
   *
   * @param ticketNum the ticket number
   * @return the name of the log file
   */
  public String getLogFile(long ticketNum) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, LOGFILE);
      return jedis.get(key);
    }
  }

  /**
   * Sets the name of the log file (original GC log data).
   *
   * @param ticketNum the ticket number
   * @param logfile the name of the log file
   */
  public void setLogFile(long ticketNum, String logfile) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, LOGFILE);
      jedis.set(key, logfile);
    }
  }

  /**
   * Sets the meta-information of log file to by analyzed.
   * @param ticketNum the ticket number
   * @param name the name of uploaded log file
   * @param size the size of log file
   */
  public void setMeta(long ticketNum, String name, long size) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, META);
      Map<String, String> data = ImmutableMap.of(
          META_NAME, name,
          META_SIZE, String.valueOf(size)
      );
      jedis.hmset(key, data);
    }
  }

  /**
   * Gets the meta data.
   * @param ticketNum the ticket number
   * @return returns the map of metadata
   */
  public Map<String, String> getMeta(long ticketNum) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.hgetAll(makeKey(ticketNum, META));
    }
  }

  /**
   * Gives the name of result file where GC analysis information is stored.
   *
   * @param ticketNum the ticket number
   * @return the name of result file
   */
  public String getResult(long ticketNum) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, RESULT);
      return jedis.get(key);
    }
  }

  /**
   * Sets the name of result file where GC analysis information is stored.
   *
   * @param ticketNum the ticket number
   * @param result the name of result file
   */
  public void setResult(long ticketNum, String result) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = makeKey(ticketNum, RESULT);
      jedis.set(key, result);
    }
  }

  /**
   * Deletes the information of the ticket entirely.
   *
   * @param ticketNum the ticket number to delete
   */
  public void deleteResource(long ticketNum) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(makeKey(ticketNum, STATUS));
      jedis.del(makeKey(ticketNum, RESULT));
      jedis.del(makeKey(ticketNum, LOGFILE));
      jedis.del(makeKey(ticketNum, META));
    }
  }

  /**
   * Close the Jedis pool application.
   */
  public void closeTicketer() {
    jedisPool.destroy();
  }
}
