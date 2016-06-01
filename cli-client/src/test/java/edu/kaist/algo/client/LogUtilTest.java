package edu.kaist.algo.client;

import static org.junit.Assert.assertEquals;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analyzer.LogAnalyzer;
import edu.kaist.algo.model.GcEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class LogUtilTest {
  private final List<GcEvent> eventList = new ArrayList<>();

  private void setUpEventList() {
    long timestamp = 10000;

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
  }

  /**
   * Setting up test.
   */
  @Before public void setUp() {
    setUpEventList();
  }

  @Test
  public void testPrintAnalyzedResult_PrintBautyfiedResult() {
    // max decimal point is 5.
    // align is done for each column, align width is (max length + 2)
    // calculated result is at edu.kaist.algo.analyzer.LogAnalyzerTest
    String expeectedOutput = ""
        + "RESULT:\n"
        + "╔════════════════╤═══════════════════╤═══════════════════╤═══════════════════╤═══════════════════╗\n"
        + "║                │ FULL_GC           │ MINOR_GC          │ CMS_INIT_MARK     │ CMS_FINAL_REMARK  ║\n"
        + "╠════════════════╪═══════════════════╪═══════════════════╪═══════════════════╪═══════════════════╣\n"
        + "║ Count          │ 50                │ 50                │ 50                │ 50                ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Total          │ 107               │ 107               │ 107               │ 107               ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Sample Mean    │ 2.14              │ 2.14              │ 2.14              │ 2.14              ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Sample Std Dev │ 1.14304           │ 1.14304           │ 1.14304           │ 1.14304           ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Sample Median  │ 2                 │ 2                 │ 2                 │ 2                 ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Mean 99%       │ 1.70679 ~ 2.57321 │ 1.70679 ~ 2.57321 │ 1.70679 ~ 2.57321 │ 1.70679 ~ 2.57321 ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Mean 95%       │ 1.81515 ~ 2.46485 │ 1.81515 ~ 2.46485 │ 1.81515 ~ 2.46485 │ 1.81515 ~ 2.46485 ║\n"
        + "╟────────────────┼───────────────────┼───────────────────┼───────────────────┼───────────────────╢\n"
        + "║ Mean 90%       │ 1.86899 ~ 2.41101 │ 1.86899 ~ 2.41101 │ 1.86899 ~ 2.41101 │ 1.86899 ~ 2.41101 ║\n"
        + "╚════════════════╧═══════════════════╧═══════════════════╧═══════════════════╧═══════════════════╝\n"
        + "\n"
        + "[FULL_GC]\n"
        + "- Min\n"
        + "10.000: Paused 1 sec\n"
        + "- Max\n"
        + "10.049: Paused 10 sec\n"
        + "- Outliers 99%\n"
        + "10.049: Paused 10 sec\n"
        + "- Outliers 90%\n"
        + "10.049: Paused 10 sec\n"
        + "- Outliers 75%\n"
        + "10.049: Paused 10 sec\n"
        + "\n"
        + "[MINOR_GC]\n"
        + "- Min\n"
        + "10.050: Paused 1 sec\n"
        + "- Max\n"
        + "10.099: Paused 10 sec\n"
        + "- Outliers 99%\n"
        + "10.099: Paused 10 sec\n"
        + "- Outliers 90%\n"
        + "10.099: Paused 10 sec\n"
        + "- Outliers 75%\n"
        + "10.099: Paused 10 sec\n"
        + "\n"
        + "[CMS_INIT_MARK]\n"
        + "- Min\n"
        + "10.100: Paused 1 sec\n"
        + "- Max\n"
        + "10.149: Paused 10 sec\n"
        + "- Outliers 99%\n"
        + "10.149: Paused 10 sec\n"
        + "- Outliers 90%\n"
        + "10.149: Paused 10 sec\n"
        + "- Outliers 75%\n"
        + "10.149: Paused 10 sec\n"
        + "\n"
        + "[CMS_FINAL_REMARK]\n"
        + "- Min\n"
        + "10.150: Paused 1 sec\n"
        + "- Max\n"
        + "10.199: Paused 10 sec\n"
        + "- Outliers 99%\n"
        + "10.199: Paused 10 sec\n"
        + "- Outliers 90%\n"
        + "10.199: Paused 10 sec\n"
        + "- Outliers 75%\n"
        + "10.199: Paused 10 sec\n"
        + "\n";

    LogAnalyzer analyzer = new LogAnalyzer(eventList);
    GcAnalyzedData data = analyzer.analyzeData();

    assertEquals(expeectedOutput, LogUtil.beautifyAnalyzedData(data));
  }
}
