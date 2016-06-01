package edu.kaist.algo.client;

import edu.kaist.algo.model.GcAnalyzedData;
import edu.kaist.algo.model.GcEvent;
import edu.kaist.algo.model.GcPauseOutliers;
import edu.kaist.algo.model.GcPauseStat;

import com.jakewharton.fliptables.FlipTableConverters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Util class for GC Log.
 */
public class LogUtil {
  private static final String RESULT_TITLE = "RESULT:\n";
  private static final String COUNT_HEADER = "Count";
  private static final String TOTAL_HEADER = "Total";
  private static final String SAMPLE_MEAN_HEADER = "Sample Mean";
  private static final String SMAPLE_STD_DEV_HEADER = "Sample Std Dev";
  private static final String SAMPLE_MEDIAN_HEADER = "Sample Median";
  private static final String MEAN_HEADER_FORMAT = "Mean %s%%";
  private static final String MEAN_FORMAT = "%s ~ %s";
  private static final String EXTRA_OUTPUT_HEADER_FORMAT = "[%s]\n";
  private static final String MIN_HEADER = "- Min\n";
  private static final String MAX_HEADER = "- Max\n";
  private static final String GC_EVENT_FORMAT = "%s.%s: Paused %s sec\n";
  private static final String OUTLIERS_HEADER_FORMAT = "- Outliers %s%%\n";

  /**
   * return beautified analysis result.
   *
   * @param analyzedData data to beautify
   * @return beautified String
   */
  public static String beautifyAnalyzedData(GcAnalyzedData analyzedData) {
    DecimalFormat headerDf = new DecimalFormat("0",
        DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    headerDf.setMaximumFractionDigits(1);

    DecimalFormat dataDf = new DecimalFormat("0",
        DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    dataDf.setMaximumFractionDigits(5);

    String[] header = new String[5];
    header[0] = "";

    int meanCount = analyzedData.getPauses(0).getMeansCount();

    Object[][] data = new Object[5 + meanCount][5];
    data[0][0] = COUNT_HEADER;
    data[1][0] = TOTAL_HEADER;
    data[2][0] = SAMPLE_MEAN_HEADER;
    data[3][0] = SMAPLE_STD_DEV_HEADER;
    data[4][0] = SAMPLE_MEDIAN_HEADER;

    for (int i = 0; i < meanCount; i++) {
      data[i + 5][0] = String.format(MEAN_HEADER_FORMAT,
          headerDf.format((1 - analyzedData.getPauses(0).getMeans(i).getLevel()) * 100));
    }

    for (GcPauseStat stat : analyzedData.getPausesList()) {
      int index = stat.getTypeValue();
      header[index] = stat.getType().toString();
      data[0][index] = stat.getCount();
      data[1][index] = dataDf.format(stat.getTotalPauseTime());
      data[2][index] = dataDf.format(stat.getSampleMean());
      data[3][index] = dataDf.format(stat.getSampleStdDev());
      data[4][index] = dataDf.format(stat.getSampleMedian());

      for (int i = 0; i < meanCount; i++) {
        data[5 + i][index] = String.format(MEAN_FORMAT,
            dataDf.format(stat.getMeans(i).getMean().getMin()),
            dataDf.format(stat.getMeans(i).getMean().getMax()));
      }
    }

    StringBuilder output = new StringBuilder()
        .append(RESULT_TITLE)
        .append(FlipTableConverters.fromObjects(header, data))
        .append('\n');

    for (GcPauseStat stat : analyzedData.getPausesList()) {
      output.append(String.format(EXTRA_OUTPUT_HEADER_FORMAT, stat.getType()));

      output.append(MIN_HEADER);
      String minTimeStamp = Long.toString(stat.getMinEvent().getTimestamp());
      output.append(String.format(GC_EVENT_FORMAT,
          minTimeStamp.substring(0, minTimeStamp.length() - 3),
          minTimeStamp.substring(minTimeStamp.length() - 3, minTimeStamp.length()),
          dataDf.format(stat.getMinEvent().getPauseTime())));

      output.append(MAX_HEADER);
      String maxTimeStamp = Long.toString(stat.getMaxEvent().getTimestamp());
      output.append(String.format(GC_EVENT_FORMAT,
          maxTimeStamp.substring(0, maxTimeStamp.length() - 3),
          maxTimeStamp.substring(maxTimeStamp.length() - 3, maxTimeStamp.length()),
          dataDf.format(stat.getMaxEvent().getPauseTime())));

      for (GcPauseOutliers outliers : stat.getOutliersList()) {
        output.append(String.format(OUTLIERS_HEADER_FORMAT,
            headerDf.format((1 - outliers.getLevel()) * 100)));
        for (GcEvent outlier : outliers.getEventsList()) {
          String timeStamp = Long.toString(outlier.getTimestamp());
          output.append(String.format(GC_EVENT_FORMAT,
              maxTimeStamp.substring(0, timeStamp.length() - 3),
              maxTimeStamp.substring(timeStamp.length() - 3, timeStamp.length()),
              dataDf.format(outlier.getPauseTime())));
        }
      }

      output.append('\n');
    }

    return output.toString();
  }
}
