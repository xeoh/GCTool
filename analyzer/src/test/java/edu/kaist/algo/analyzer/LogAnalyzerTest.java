/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.analyzer;

import static org.junit.Assert.assertEquals;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.model.GcEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class LogAnalyzerTest {
  private List<GcEvent> eventList = new ArrayList<>();

  /**
   * Setting up test.
   */
  @Before public void setUp() {
    long timestamp = 0;

    // Setting FULL_GC type events
    // adding one FULL_GC event with pause time 1.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.FULL_GC)
        .setTimestamp(timestamp).setPauseTime(1.0).build());
    timestamp += 1;

    // adding 48 FULL_GC event with pause time 2.0 sec
    for (int i = 0; i < 48; i++) {
      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.FULL_GC)
          .setTimestamp(timestamp).setPauseTime(2.0).build());
      timestamp += 1;
    }

    // adding one FULL_GC event with pause time 10.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.FULL_GC)
        .setTimestamp(timestamp).setPauseTime(10.0).build());
    timestamp += 1;

    // Setting MINOR_GC type events
    // adding one MINOR_GC event with pause time 1.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.MINOR_GC)
        .setTimestamp(timestamp).setPauseTime(1.0).build());
    timestamp += 1;

    // adding 50 MINOR_GC event with pause time 2.0 sec
    for (int i = 0; i < 48; i++) {
      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.MINOR_GC)
          .setTimestamp(timestamp).setPauseTime(2.0).build());
      timestamp += 1;
    }

    // adding one MINOR_GC event with pause time 10.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.MINOR_GC)
        .setTimestamp(timestamp).setPauseTime(10.0).build());
    timestamp += 1;

    // Setting CMS_INIT_MARK type events
    // adding one CMS_INIT_MARK event with pause time 1.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_INIT_MARK)
        .setTimestamp(timestamp).setPauseTime(1.0).build());
    timestamp += 1;

    // adding 50 CMS_INIT_MARK event with pause time 2.0 sec
    for (int i = 0; i < 48; i++) {
      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_INIT_MARK)
          .setTimestamp(timestamp).setPauseTime(2.0).build());
      timestamp += 1;
    }

    // adding one CMS_INIT_MARK event with pause time 10.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_INIT_MARK)
        .setTimestamp(timestamp).setPauseTime(10.0).build());
    timestamp += 1;

    // Setting CMS_FINAL_REMARK type events
    // adding one CMS_FINAL_REMARK event with pause time 1.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_FINAL_REMARK)
        .setTimestamp(timestamp).setPauseTime(1.0).build());
    timestamp += 1;

    // adding 50 CMS_FINAL_REMARK event with pause time 2.0 sec
    for (int i = 0; i < 48; i++) {
      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_FINAL_REMARK)
          .setTimestamp(timestamp).setPauseTime(2.0).build());
      timestamp += 1;
    }

    // adding one CMS_FINAL_REMARK event with pause time 10.0 sec
    eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_FINAL_REMARK)
        .setTimestamp(timestamp).setPauseTime(10.0).build());
    timestamp += 1;

    // Setting CMS_CONCURRENT evnets
    for (int i = 0; i < 20; i++) {
      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-mark-start").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-mark").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-preclean-start").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-preclean").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-abortable-preclean-start").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-abortable-preclean").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-sweep-start").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-sweep").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-reset-start").build());
      timestamp += 1;

      eventList.add(GcEvent.newBuilder().setLogType(GcEvent.LogType.CMS_CONCURRENT)
          .setTimestamp(timestamp).setCmsCpuTime(0.1).setCmsWallTime(0.2)
          .setTypeDetail("CMS-concurrent-reset").build());
      timestamp += 1;
    }
  }

  @Test
  public void analyzeData_returnAnalyzedData() {
    /* Hand-calculated stat for each of metric.
     * value for each type: FULL_GC = 1, MINOR_GC = 2, CMS_INIT_MARK = 3, CMS_FINAL_REMARK = 4
     * count: 50
     * total pause time: 107.0 sec
     * sample mean: 2.14
     * sample std_dev: 1.14304
     * sample median: 2
     * min event: timestamp = 0 + (LOG_TYPE - 1) * 50 // pause time = 1.0
     * max event: timestamp = LOG_TYPE * 50 - 1       // pause time = 10.0
     *
     * means: level = 0.01  // 1.70679 ≤ x ≤ 2.57321
     *        level = 0.05  // 1.81515 ≤ x ≤ 2.46485
     *        level = 0.1   // 1.86899 ≤ x ≤ 2.41101
     *
     * outliers: level = 0.01 // only max event is outlier
     *           level = 0.1  // only max event is outlier
     *           level = 0.25 // only max event is outlier
     */

    LogAnalyzer analyzer = new LogAnalyzer(eventList);
    GcAnalyzedData data = analyzer.analyzeData();

    for (int i = 0; i < 4; i++) {
      assertEquals(i + 1, data.getPauses(i).getTypeValue());
      assertEquals(50, data.getPauses(i).getCount());
      assertEquals(107.0, data.getPauses(i).getTotalPauseTime(), 0.001);
      assertEquals(2.14, data.getPauses(i).getSampleMean(), 0.001);
      assertEquals(1.14304, data.getPauses(i).getSampleStdDev(), 0.001);
      assertEquals(2.0, data.getPauses(i).getSampleMedian(), 0.001);
      assertEquals(i * 50, data.getPauses(i).getMinEvent().getTimestamp());
      assertEquals(1.0, data.getPauses(i).getMinEvent().getPauseTime(), 0.001);
      assertEquals((i + 1) * 50 - 1, data.getPauses(i).getMaxEvent().getTimestamp());
      assertEquals(10.0, data.getPauses(i).getMaxEvent().getPauseTime(), 0.001);

      assertEquals(0.01, data.getPauses(i).getMeans(0).getLevel(), 0.001);
      assertEquals(1.70679, data.getPauses(i).getMeans(0).getMean().getMin(), 0.001);
      assertEquals(2.57321, data.getPauses(i).getMeans(0).getMean().getMax(), 0.001);

      assertEquals(0.05, data.getPauses(i).getMeans(1).getLevel(), 0.001);
      assertEquals(1.81515, data.getPauses(i).getMeans(1).getMean().getMin(), 0.001);
      assertEquals(2.46485, data.getPauses(i).getMeans(1).getMean().getMax(), 0.001);

      assertEquals(0.1, data.getPauses(i).getMeans(2).getLevel(), 0.001);
      assertEquals(1.86899, data.getPauses(i).getMeans(2).getMean().getMin(), 0.001);
      assertEquals(2.41101, data.getPauses(i).getMeans(2).getMean().getMax(), 0.001);

      assertEquals(0.01, data.getPauses(i).getOutliers(0).getLevel(), 0.001);
      assertEquals(1, data.getPauses(i).getOutliers(0).getEventsCount());
      assertEquals((i + 1) * 50 - 1, data.getPauses(i).getOutliers(0).getEvents(0).getTimestamp());
      assertEquals(10.0, data.getPauses(i).getOutliers(0).getEvents(0).getPauseTime(), 0.001);

      assertEquals(0.1, data.getPauses(i).getOutliers(1).getLevel(), 0.001);
      assertEquals(1, data.getPauses(i).getOutliers(1).getEventsCount());
      assertEquals((i + 1) * 50 - 1, data.getPauses(i).getOutliers(1).getEvents(0).getTimestamp());
      assertEquals(10.0, data.getPauses(i).getOutliers(1).getEvents(0).getPauseTime(), 0.001);

      assertEquals(0.25, data.getPauses(i).getOutliers(2).getLevel(), 0.001);
      assertEquals(1, data.getPauses(i).getOutliers(2).getEventsCount());
      assertEquals((i + 1) * 50 - 1, data.getPauses(i).getOutliers(2).getEvents(0).getTimestamp());
      assertEquals(10.0, data.getPauses(i).getOutliers(2).getEvents(0).getPauseTime(), 0.001);
    }

    assertEquals(10, data.getConcurrencesCount());

    assertEquals(20, data.getConcurrences(0).getCount());
    assertEquals("CMS-concurrent-mark-start", data.getConcurrences(0).getTypeDetail());

    assertEquals(20, data.getConcurrences(1).getCount());
    assertEquals("CMS-concurrent-mark", data.getConcurrences(1).getTypeDetail());

    assertEquals(20, data.getConcurrences(2).getCount());
    assertEquals("CMS-concurrent-preclean-start", data.getConcurrences(2).getTypeDetail());

    assertEquals(20, data.getConcurrences(3).getCount());
    assertEquals("CMS-concurrent-preclean", data.getConcurrences(3).getTypeDetail());

    assertEquals(20, data.getConcurrences(4).getCount());
    assertEquals("CMS-concurrent-abortable-preclean-start",
        data.getConcurrences(4).getTypeDetail());

    assertEquals(20, data.getConcurrences(5).getCount());
    assertEquals("CMS-concurrent-abortable-preclean", data.getConcurrences(5).getTypeDetail());

    assertEquals(20, data.getConcurrences(6).getCount());
    assertEquals("CMS-concurrent-sweep-start", data.getConcurrences(6).getTypeDetail());

    assertEquals(20, data.getConcurrences(7).getCount());
    assertEquals("CMS-concurrent-sweep", data.getConcurrences(7).getTypeDetail());

    assertEquals(20, data.getConcurrences(8).getCount());
    assertEquals("CMS-concurrent-reset-start", data.getConcurrences(8).getTypeDetail());

    assertEquals(20, data.getConcurrences(9).getCount());
    assertEquals("CMS-concurrent-reset", data.getConcurrences(9).getTypeDetail());
  }
}
