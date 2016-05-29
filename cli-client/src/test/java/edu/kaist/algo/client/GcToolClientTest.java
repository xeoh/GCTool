package edu.kaist.algo.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.commons.cli.Options;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests the functionality of parsing arguments for the client.
 */
@RunWith(JUnit4.class)
public class GcToolClientTest {
  private final Options options = GcToolClient.makeOptions();

  /**
   * Takes various combinations of options given, and tests for their validity.
   */
  @Test
  public void testParseOptions() {
    String[] args;
    GcToolClient.ParsedOptions parsedopt;

    // working options
    args = new String[] {"-h", "localhost", "-p", "50051", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNotNull(parsedopt);
    assertEquals("localhost", parsedopt.getHost());
    assertEquals(50051, parsedopt.getPort());
    assertEquals("logfile.log", parsedopt.getFilename());

    // working options, given IP address as host
    args = new String[] {"-h", "148.255.255.1", "-p", "50051", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNotNull(parsedopt);
    assertEquals("148.255.255.1", parsedopt.getHost());
    assertEquals(50051, parsedopt.getPort());
    assertEquals("logfile.log", parsedopt.getFilename());

    // working options, implicit host = localhost
    args = new String[] {"-p", "50051", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNotNull(parsedopt);
    assertEquals("localhost", parsedopt.getHost());
    assertEquals(50051, parsedopt.getPort());
    assertEquals("logfile.log", parsedopt.getFilename());

    // working options, implicit host = localhost, no file option
    args = new String[] {"-p", "50051"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNotNull(parsedopt);
    assertEquals("localhost", parsedopt.getHost());
    assertEquals(50051, parsedopt.getPort());

    // not existing port number
    args = new String[] {"-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // invalid port number
    args = new String[] {"-p", "-1", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // invalid port number2
    args = new String[] {"-p", "99999", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // invalid port number3
    args = new String[] {"-p", "not a number", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // not existing option
    args = new String[] {"-u", "50051", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // not existing argument to an option
    args = new String[] {"-p", "-h"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // empty string argument
    args = new String[] {"-h", "", "-p", "50051", "-f", "logfile.log"};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);

    // empty string argument 2
    args = new String[] {"-p", "50051", "-f", ""};
    parsedopt = GcToolClient.parseOptions(options, args);
    assertNull(parsedopt);
  }
}
