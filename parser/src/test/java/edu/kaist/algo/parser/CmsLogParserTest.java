/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.parser;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Resources;

import edu.kaist.algo.model.GcEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class CmsLogParserTest {

  private final CmsLogParser parser = new CmsLogParser();

  @Test
  public void testParseMinorGc() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "126.426: [GC (Allocation Failure) 126.426: [ParNew: 30720K-&gt;3392K(30720K), 0.0128764 secs] 83156K-&gt;58798K(99008K), 0.0131301 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "126.487: [GC (Allocation Failure) 126.487: [ParNew: 30720K-&gt;3392K(30720K), 0.0110704 secs] 86126K-&gt;61977K(99008K), 0.0113176 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(2, result.size());
    assertGcEvent(result.get(0), GcEvent.LogType.MINOR_GC, 11779, 126426, 0.0131301, 0.02, 0.00, 0.01);
    assertGcEvent(result.get(1), GcEvent.LogType.MINOR_GC, 11779, 126487, 0.0113176, 0.02, 0.00, 0.01);
  }

  @Test
  public void testParseFullGcSystem() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "111.350: [GC (Allocation Failure) 111.351: [ParNew: 30720K-&gt;3392K(30720K), 0.0095220 secs] 75542K-&gt;50511K(99008K), 0.0097545 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "111.499: [GC (Allocation Failure) 111.499: [ParNew: 30720K-&gt;3391K(30720K), 0.0105871 secs] 77839K-&gt;53229K(99008K), 0.0108695 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "111.569: [Full GC (System.gc()) 111.569: [CMS: 49837K-&gt;38462K(68288K), 0.1817762 secs] 65053K-&gt;38462K(99008K), [Metaspace: 60946K-&gt;60946K(1105920K)], 0.1819922 secs] [Times: user=0.18 sys=0.00, real=0.18 secs]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(3, result.size());
    assertGcEvent(result.get(2), GcEvent.LogType.FULL_GC, 11779, 111569, 0.1819922, 0.18, 0.00, 0.18);
  }

  @Test
  public void testParseConcurrentModeFailure() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "126.426: [GC (Allocation Failure) 126.426: [ParNew: 30720K-&gt;3392K(30720K), 0.0128764 secs] 83156K-&gt;58798K(99008K), 0.0131301 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "126.487: [GC (Allocation Failure) 126.487: [ParNew: 30720K-&gt;3392K(30720K), 0.0110704 secs] 86126K-&gt;61977K(99008K), 0.0113176 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "126.554: [GC (Allocation Failure) 126.554: [ParNew: 30720K-&gt;3392K(30720K), 0.0141218 secs] 89305K-&gt;65368K(99008K), 0.0144143 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "126.619: [GC (Allocation Failure) 126.619: [ParNew: 30720K-&gt;3391K(30720K), 0.0120420 secs] 92696K-&gt;67456K(99008K), 0.0122437 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "126.679: [GC (Allocation Failure) 126.679: [ParNew: 30719K-&gt;3391K(30720K), 0.0127160 secs] 94784K-&gt;69858K(99008K), 0.0129960 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]\n"
        + "126.743: [GC (Allocation Failure) 126.743: [ParNew: 30719K-&gt;30719K(30720K), 0.0000574 secs]126.743: [CMS\n"
        + "<writer thread='11267'/>\n"
        + "126.757: [CMS-concurrent-mark: 0.304/0.377 secs] [Times: user=1.08 sys=0.13, real=0.38 secs]\n"
        + "<writer thread='11779'/>\n"
        + " (concurrent mode failure): 66466K-&gt;45817K(68288K), 0.2277434 secs] 97186K-&gt;45817K(99008K), [Metaspace: 60953K-&gt;60953K(1105920K)], 0.2280550 secs] [Times: user=0.23 sys=0.00, real=0.22 secs]\n"
        + "127.029: [GC (Allocation Failure) 127.030: [ParNew: 27319K-&gt;3391K(30720K), 0.0117355 secs] 73136K-&gt;51371K(99008K), 0.0119592 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(8, result.size());
    assertGcEvent(result.get(6), GcEvent.LogType.FULL_GC, 11779, 126743, 0.2280550, 0.23, 0.00, 0.22);
  }

  @Test
  public void testParseFullGc() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "126.743: [GC (Allocation Failure) 126.743: [ParNew: 30719K-&gt;30719K(30720K), 0.0000574 secs]126.743: [CMS"
        + ": 66466K-&gt;45817K(68288K), 0.2277434 secs] 97186K-&gt;45817K(99008K), [Metaspace: 60953K-&gt;60953K(1105920K)], 0.2280550 secs] [Times: user=0.23 sys=0.00, real=0.22 secs]\n"
        + "127.029: [Full GC 127.029: [CMS: 49837K-&gt;38462K(68288K), 0.1817762 secs] 65053K-&gt;38462K(99008K), [Metaspace: 60946K-&gt;60946K(1105920K)], 0.1819922 secs] [Times: user=0.18 sys=0.00, real=0.18 secs]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(2, result.size());
    assertGcEvent(result.get(0), GcEvent.LogType.FULL_GC, 11779, 126743, 0.2280550, 0.23, 0.00, 0.22);
    assertGcEvent(result.get(1), GcEvent.LogType.FULL_GC, 11779, 127029, 0.1819922, 0.18, 0.00, 0.18);
  }

  @Test
  public void testParseFullGcConcurrentModeFailure() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "55.780: [Full GC (Allocation Failure) 55.780: [CMS\n"
        + "<writer thread='11267'/>\n"
        + "55.799: [CMS-concurrent-mark: 0.113/0.158 secs] [Times: user=0.66 sys=0.08, real=0.15 secs]\n"
        + "<writer thread='11779'/>\n"
        + " (concurrent mode failure): 64750K-&gt;45276K(68288K), 0.1975301 secs] 95470K-&gt;45276K(99008K), [Metaspace: 61093K-&gt;61093K(1107968K)], 0.1979402 secs] [Times: user=0.19 sys=0.00, real=0.20 secs]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(2, result.size());
    assertGcEvent(result.get(1), GcEvent.LogType.FULL_GC, 11779, 55780, 0.1979402, 0.19, 0.00, 0.20);
  }

  @Test
  public void testParseInitialMark() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "127.930: [GC (Allocation Failure) 127.930: [ParNew: 30671K-&gt;3392K(30720K), 0.0104972 secs] 79360K-&gt;54981K(99008K), 0.0106961 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]\n"
        + "127.942: [GC (CMS Initial Mark) [1 CMS-initial-mark: 51589K(68288K)] 55338K(99008K), 0.0025420 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]\n"
        + "<writer thread='11267'/>\n"
        + "127.945: [CMS-concurrent-mark-start]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(3, result.size());
    assertGcEvent(result.get(1), GcEvent.LogType.CMS_INIT_MARK, 11779, 127942, 0.0025420, 0.01, 0.00, 0.00);
  }

  @Test
  public void testParseRemark() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "127.794: [GC (CMS Final Remark) [YG occupancy: 19543 K (30720 K)]127.795: [Rescan (parallel) , 0.0081128 secs]127.803: [weak refs processing, 0.0015528 secs]127.804: [class unloading, 0.0164919 secs]127.821: [scrub symbol table, 0.0126534 secs]127.834: [scrub string table, 0.0010522 secs][1 CMS-remark: 58240K(68288K)] 77784K(99008K), 0.0402029 secs] [Times: user=0.06 sys=0.01, real=0.04 secs]\n"
        + "<writer thread='11267'/>\n"
        + "127.835: [CMS-concurrent-sweep-start]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(2, result.size());
    assertGcEvent(result.get(0), GcEvent.LogType.CMS_FINAL_REMARK, 11779, 127794, 0.0402029, 0.06, 0.01, 0.04);
  }

  @Test
  public void testParseLogFile() throws Exception {
    final Path path = Paths.get(Resources.getResource("hotspot_short.log").toURI());
    final List<GcEvent> result = parser.parse(path);
    final long fullGcCount = result.stream().filter(e -> e.getLogType() == GcEvent.LogType.FULL_GC).count();
    assertEquals(5, fullGcCount); // Full GC : 4, GC -> ParNew -> CMS (concurrent mode failure) : 1
    final long gcEventCountWithoutCmsConcurrent =
        result.stream().filter(e -> e.getLogType() != GcEvent.LogType.CMS_CONCURRENT).count();
    assertEquals(258, gcEventCountWithoutCmsConcurrent - fullGcCount); // Total : 263
  }

  @Test
  public void testCMSConcurrent() throws Exception {
    final String log = "<writer thread='11267'/>\n"
        + "25.908: [CMS-concurrent-sweep-start]\n"
        + "25.969: [CMS-concurrent-sweep: 0.061/0.061 secs] [Times: user=0.21 sys=0.00, real=0.06 secs] \n"
        + "25.969: [CMS-concurrent-reset-start]\n"
        + "25.969: [CMS-concurrent-reset: 0.000/0.000 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(4, result.size());
    assertEquals(GcEvent.LogType.CMS_CONCURRENT, result.get(0).getLogType());

    assertEquals(GcEvent.LogType.CMS_CONCURRENT, result.get(1).getLogType());
    assertEquals(11267, result.get(1).getThread());
    assertEquals(0.061, result.get(1).getCmsCpuTime(), 0.0001);
    assertEquals(0.061, result.get(1).getCmsWallTime(), 0.0001);
    assertEquals(0.21, result.get(1).getUserTime(), 0.0001);
    assertEquals(0.06, result.get(1).getRealTime(), 0.0001);
    assertEquals("CMS-concurrent-sweep", result.get(1).getTypeDetail());

    assertEquals(GcEvent.LogType.CMS_CONCURRENT, result.get(3).getLogType());
    assertEquals(11267, result.get(3).getThread());
    assertEquals(0.0, result.get(3).getCmsCpuTime(), 0.0001);
    assertEquals(0.0, result.get(3).getCmsWallTime(), 0.0001);
    assertEquals(0.01, result.get(3).getUserTime(), 0.0001);
    assertEquals(0.00, result.get(3).getRealTime(), 0.0001);
    assertEquals("CMS-concurrent-reset", result.get(3).getTypeDetail());
  }

  @Test
  public void testTypeDetail() throws Exception {
    final String log = "<writer thread='11779'/>\n"
        + "55.780: [Full GC (Allocation Failure) 55.780: [CMS\n"
        + "<writer thread='11267'/>\n"
        + "55.799: [CMS-concurrent-mark: 0.113/0.158 secs] [Times: user=0.66 sys=0.08, real=0.15 secs]\n"
        + "<writer thread='11779'/>\n"
        + " (concurrent mode failure): 64750K-&gt;45276K(68288K), 0.1975301 secs] 95470K-&gt;45276K(99008K), [Metaspace: 61093K-&gt;61093K(1107968K)], 0.1979402 secs] [Times: user=0.19 sys=0.00, real=0.20 secs]\n"
        + "127.794: [GC (CMS Final Remark) [YG occupancy: 19543 K (30720 K)]127.795: [Rescan (parallel) , 0.0081128 secs]127.803: [weak refs processing, 0.0015528 secs]127.804: [class unloading, 0.0164919 secs]127.821: [scrub symbol table, 0.0126534 secs]127.834: [scrub string table, 0.0010522 secs][1 CMS-remark: 58240K(68288K)] 77784K(99008K), 0.0402029 secs] [Times: user=0.06 sys=0.01, real=0.04 secs]\n"
        + "<writer thread='11267'/>\n"
        + "127.835: [CMS-concurrent-sweep-start]";
    final List<GcEvent> result = parser.parse(Stream.of(log.split("\n")));
    assertEquals(4, result.size());
    assertEquals("CMS-concurrent-mark", result.get(0).getTypeDetail());
    assertEquals("Full GC (Allocation Failure); CMS (concurrent mode failure); Metaspace", result.get(1).getTypeDetail());
    assertEquals("GC (CMS Final Remark); YG occupancy; Rescan (parallel); weak refs processing; class unloading; scrub symbol table; scrub string table; 1 CMS-remark", result.get(2).getTypeDetail());
    assertEquals("CMS-concurrent-sweep-start", result.get(3).getTypeDetail());
  }

  private static void assertGcEvent(GcEvent event, GcEvent.LogType logType, int thread, long timestamp,
                                    double pauseTime, double user, double sys, double real) {
    assertEquals(thread, event.getThread());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(pauseTime, event.getPauseTime(), 0.0001);
    assertEquals(logType, event.getLogType());
    assertEquals(user, event.getUserTime(), 0.0001);
    assertEquals(sys, event.getSysTime(), 0.0001);
    assertEquals(real, event.getRealTime(), 0.0001);
  }
}
