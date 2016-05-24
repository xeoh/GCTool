package edu.kaist.algo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.fiftyonred.mock_jedis.MockJedisPool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Test the Ticketer class by using a MockJedis class so that
 * the test is available without actual redis server locally turned on.
 */
@RunWith(JUnit4.class)
public class TicketerTest {
  private static final Ticketer.Status EXAMPLE_STATUS =
      Ticketer.Status.ANALYZING;
  private static final String EXAMPLE_LOGFILE = "example.log";
  private static final String EXAMPLE_META = "metafile";
  private static final String EXAMPLE_RESULT = "resultfile";

  private static Ticketer ticketer;
  private static long ticket;

  /**
   * Set up the test. Create a mock Ticketer instance, and
   * issue one ticket. Then set for that ticket each resources.
   */
  @Before
  public void setUp() {
    JedisPool jedisPool = new MockJedisPool(new JedisPoolConfig(), "localhost");
    ticketer = new Ticketer(jedisPool); // MockJedisPool constructor
    ticket = ticketer.issueTicket(); // this should be 1
    ticketer.setLogFile(ticket, EXAMPLE_LOGFILE);
    ticketer.setMeta(ticket, EXAMPLE_META);
    ticketer.setResult(ticket, EXAMPLE_RESULT);
    ticketer.setStatus(ticket, EXAMPLE_STATUS);
  }

  // issues another ticket and checks its value
  @Test
  public void issueTicketTest() {
    long ticket = ticketer.issueTicket();
    assertEquals(2, ticket);
  }

  // given illegal ticket number
  @Test(expected = IllegalArgumentException.class)
  public void wrongTicketNumTest() {
    long ticketnum = 0;
    Ticketer.makeKey(ticketnum, "resource");
    fail();
  }

  // given illegal ticket number
  @Test(expected = IllegalArgumentException.class)
  public void wrongTicketNumTest2() {
    long ticketnum = -1;
    Ticketer.makeKey(ticketnum, "resource");
    fail();
  }

  // given illegal resource name
  @Test(expected = IllegalArgumentException.class)
  public void wrongResourceNameTest() {
    long ticketnum = 1;
    Ticketer.makeKey(ticketnum, "blahblah");
    fail();
  }

  // the status stored does not exit
  @Test
  public void nullEnumStatusTest() {
    Ticketer.Status status = ticketer.getStatus(2);
    assertNull(status);
  }

  // create keys for various resource names and ticket numbers
  @Test
  public void makeTicketTest() {
    String ticketString;
    ticketString = Ticketer.makeKey(1, Ticketer.STATUS);
    assertEquals("ticket:1:status", ticketString);

    ticketString = Ticketer.makeKey(3, Ticketer.META);
    assertEquals("ticket:3:meta", ticketString);

    ticketString = Ticketer.makeKey(10, Ticketer.LOGFILE);
    assertEquals("ticket:10:logfile", ticketString);

    ticketString = Ticketer.makeKey(5, Ticketer.RESULT);
    assertEquals("ticket:5:result", ticketString);
  }

  // check if the resource is properly contained in redis
  @Test
  public void getResourceTest() {
    Ticketer.Status status = ticketer.getStatus(ticket);
    assertEquals(EXAMPLE_STATUS, status);

    String logfileName = ticketer.getLogFile(ticket);
    assertEquals(EXAMPLE_LOGFILE, logfileName);

    String metafileName = ticketer.getMeta(ticket);
    assertEquals(EXAMPLE_META, metafileName);

    String resultName = ticketer.getResult(ticket);
    assertEquals(EXAMPLE_RESULT, resultName);
  }

  /**
   * Delete the resources and close the ticketer instance.
   * On the way, test if the resources are deleted appropriately.
   */
  @After
  public void deleteResourceTest() {
    ticketer.deleteResource(ticket);
    assertNull(ticketer.getStatus(ticket));

    String logfileName = ticketer.getLogFile(ticket);
    assertNull(logfileName);

    String metafileName = ticketer.getMeta(ticket);
    assertNull(metafileName);

    String resultName = ticketer.getResult(ticket);
    assertNull(resultName);

    ticketer.closeTicketer();
  }
}
