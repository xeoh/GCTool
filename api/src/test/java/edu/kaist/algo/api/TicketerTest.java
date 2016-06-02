package edu.kaist.algo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;

import com.fiftyonred.mock_jedis.MockJedisPool;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analysis.GcPauseStat;
import edu.kaist.algo.model.GcEvent;
import edu.kaist.algo.service.AnalysisStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;


/**
 * Test the Ticketer class by using a MockJedis class so that
 * the test is available without actual redis server locally turned on.
 */
@RunWith(JUnit4.class)
public class TicketerTest {
  private static final AnalysisStatus EXAMPLE_STATUS = AnalysisStatus.ANALYZING;
  private static final String EXAMPLE_LOGFILE = "example.log";
  private static final GcAnalyzedData EXAMPLE_RESULT = GcAnalyzedData.newBuilder().addPauses(
      GcPauseStat.newBuilder().setType(GcEvent.LogType.MINOR_GC).build()).build();
  private static final long EXAMPLE_SIZE = 6778;

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
    ticketer.setResult(ticket, EXAMPLE_RESULT);
    ticketer.setStatus(ticket, EXAMPLE_STATUS);
    ticketer.setMeta(ticket, EXAMPLE_LOGFILE, EXAMPLE_SIZE);
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
    AnalysisStatus status = ticketer.getStatus(2);
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
    AnalysisStatus status = ticketer.getStatus(ticket);
    assertEquals(EXAMPLE_STATUS, status);

    String logfileName = ticketer.getLogFile(ticket);
    assertEquals(EXAMPLE_LOGFILE, logfileName);

    Map<String, String> metaKey = ticketer.getMeta(ticket);
    Map<String, String> data = ImmutableMap.of(
        Ticketer.META_NAME, EXAMPLE_LOGFILE,
        Ticketer.META_SIZE, String.valueOf(EXAMPLE_SIZE)
    );
    assertEquals(data, metaKey);

    assertEquals(EXAMPLE_RESULT, ticketer.getResult(ticket));
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

    Map<String, String> metaKey = ticketer.getMeta(ticket);
    assertTrue(metaKey.isEmpty());

    assertNull(ticketer.getResult(ticket));

    ticketer.closeTicketer();
  }
}
