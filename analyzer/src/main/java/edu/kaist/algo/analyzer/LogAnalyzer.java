package edu.kaist.algo.analyzer;

import com.google.common.collect.ImmutableList;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analysis.GcConcurrentStat;
import edu.kaist.algo.analysis.GcEstimatedPauseTime;
import edu.kaist.algo.analysis.GcPauseOutliers;
import edu.kaist.algo.analysis.GcPauseStat;
import edu.kaist.algo.model.GcEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for Analyze the log.
 */
public class LogAnalyzer {
  private static final List<String> concurrentOrder = ImmutableList.of(
      "CMS-concurrent-mark-start",
      "CMS-concurrent-mark-start",
      "CMS-concurrent-mark",
      "CMS-concurrent-preclean-start",
      "CMS-concurrent-preclean",
      "CMS-concurrent-abortable-preclean-start",
      "CMS-concurrent-abortable-preclean",
      "CMS-concurrent-sweep-start",
      "CMS-concurrent-sweep",
      "CMS-concurrent-reset-start",
      "CMS-concurrent-reset"
  );

  private List<GcEvent> gcEvents;

  /**
   * Constructor of LogAnalyzer.
   *
   * @param events list of gc events to analyze
   */
  public LogAnalyzer(List<GcEvent> events) {
    gcEvents = events;
  }

  /**
   * Analyze GcEvents. Estimate mean with level 0.01, 0.05, 0.1
   * Detect outliers with level 0.01, 0.1, 0.25
   *
   * @return analyzed data
   */
  public GcAnalyzedData analyzeData() {
    return analyzeData(new Double[] { 0.01, 0.05, 0.1 }, new Double[] { 0.01, 0.1, 0.25 });
  }

  /**
   * Analyze GcEvents with given levels.
   *
   * @param meanLevels levels to estimating mean.
   * @param outlierLevels levels to detect outliers.
   * @return analyzed data
   */
  public GcAnalyzedData analyzeData(Double[] meanLevels, Double[] outlierLevels) {
    return GcAnalyzedData.newBuilder()
        .addPauses(analyzePauseTime(GcEvent.LogType.FULL_GC, meanLevels, outlierLevels))
        .addPauses(analyzePauseTime(GcEvent.LogType.MINOR_GC, meanLevels, outlierLevels))
        .addPauses(analyzePauseTime(GcEvent.LogType.CMS_INIT_MARK, meanLevels, outlierLevels))
        .addPauses(analyzePauseTime(GcEvent.LogType.CMS_FINAL_REMARK, meanLevels, outlierLevels))
        .addAllConcurrences(analyzedConcurrentEvents())
        .build();
  }

  private GcPauseStat analyzePauseTime(GcEvent.LogType type,
                                       Double[] meanLevels, Double[] outlierLevels) {
    List<GcEvent> data = gcEvents.stream()
        .filter(e -> e.getLogType() == type)
        .collect(Collectors.toList());
    List<Double> pauseTimeList = data.stream()
        .mapToDouble(GcEvent::getPauseTime)
        .boxed().collect(Collectors.toList());

    final double totalTime = Statistics.getTotalSum(pauseTimeList);
    final double sampleMean = Statistics.getSampleMean(pauseTimeList);
    final double sampleStdDev = Statistics.getSampleStdDev(pauseTimeList, sampleMean);
    final double sampleMedian = Statistics.getSampleMedian(pauseTimeList);
    final GcEvent min = Statistics.getMin(data, GcEvent::getPauseTime);
    final GcEvent max = Statistics.getMax(data, GcEvent::getPauseTime);

    ArrayList<GcEstimatedPauseTime> means = new ArrayList<>();
    for (Double meanLevel : meanLevels) {
      means.add(GcEstimatedPauseTime.newBuilder()
          .setLevel(meanLevel)
          .setMean(Statistics.estimateMean(sampleMean, sampleStdDev, data.size(), meanLevel))
          .build());
    }

    ArrayList<GcPauseOutliers> outliers = new ArrayList<>();
    for (Double outlierLevel : outlierLevels) {
      outliers.add(GcPauseOutliers.newBuilder()
          .setLevel(outlierLevel).addAllEvents(Statistics.getOutliers(data, sampleMean,
              sampleStdDev, data.size(), outlierLevel, GcEvent::getPauseTime))
          .build());
    }

    return GcPauseStat.newBuilder()
        .setType(type)
        .setCount(data.size())
        .setTotalPauseTime(totalTime)
        .setSampleMean(sampleMean)
        .setSampleStdDev(sampleStdDev)
        .setSampleMedian(sampleMedian)
        .setMinEvent(min)
        .setMaxEvent(max)
        .addAllMeans(means)
        .addAllOutliers(outliers)
        .build();
  }

  private List<GcConcurrentStat> analyzedConcurrentEvents() {
    return gcEvents.stream()
        .filter(e -> e.getLogType() == GcEvent.LogType.CMS_CONCURRENT)
        .collect(Collectors.groupingBy(GcEvent::getTypeDetail, Collectors.counting()))
        .entrySet().stream()
        .sorted((c1, c2) -> Integer.compare(
            concurrentOrder.indexOf(c1.getKey()), concurrentOrder.indexOf(c2.getKey())))
        .map(e -> GcConcurrentStat.newBuilder()
            .setTypeDetail(e.getKey())
            .setCount(e.getValue().intValue())
            .build()).collect(Collectors.toList());
  }
}
