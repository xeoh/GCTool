/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.analyzer;

import edu.kaist.algo.statistics.MeanRange;

import org.apache.commons.math3.distribution.TDistribution;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Statistics class can calculating statistical things.
 *
 * <ul>
 *   <li>Get Min</li>
 *   <li>Get Max</li>
 *   <li>Calculate Total Sum</li>
 *   <li>Calculate Sample Mean</li>
 *   <li>Calculate Sample Median</li>
 *   <li>Calculate Sample Variance</li>
 *   <li>Calculate Sample Standard Variance</li>
 *   <li>Estimate Mean</li>
 *   <li>Detect Outliers</li>
 * </ul>
 */
public class Statistics {
  private static final String NOT_ENOUGH_DATA_1 = "Not enough data, should be more than one.";
  private static final String NOT_ENOUGH_DATA_2 = "Not enough data, should be more than two.";
  private static final String LEVEL_OUT_OF_RANGE = "Level is out of range.";
  private static final String NO_DATA = "No data to compute";

  interface Extractor<T> {
    double getValue(T object);
  }

  /**
   * Get data which has min value.
   *
   * @param data sample data set <font color=orange>(empty list is not allowed)</font>
   * @param extractor function to get value
   * @param <T> Type of data
   * @return minimum valued data
   */
  public static <T> T getMin(List<T> data, Extractor<T> extractor) {
    if (data.size() < 1) {
      throw new IllegalArgumentException(NO_DATA);
    }

    return data.stream()
        .min((e1, e2) -> Double.compare(extractor.getValue(e1), extractor.getValue(e2)))
        .get();
  }

  /**
   * Get data which has max value.
   *
   * @param data sample data set <font color=orange>(empty list is not allowed)</font>
   * @param extractor function to get value
   * @param <T> Type of data
   * @return maximum valued data
   */
  public static <T> T getMax(List<T> data, Extractor<T> extractor) {
    if (data.size() < 1) {
      throw new IllegalArgumentException(NO_DATA);
    }

    return data.stream()
        .max((e1, e2) -> Double.compare(extractor.getValue(e1), extractor.getValue(e2)))
        .get();
  }

  /**
   * Calculate Total Sum of data.
   *
   * @param data sample data set
   * @return Total Sum.
   */
  public static double getTotalSum(List<Double> data) {
    return data.stream().reduce(0.0, Double::sum);
  }

  /**
   * Calculate Sample Mean of data.
   *
   * @param data sample data set <font color=orange>(empty list is not allowed)</font>
   * @return Sample Mean.
   */
  public static double getSampleMean(List<Double> data) {
    if (data.size() < 1) {
      return 0;
    }

    return data.stream().reduce(0.0, Double::sum) / data.size();
  }

  /**
   * Calculate Sample Median of data.
   *
   * @param data sample data set <font color=orange>(empty list is not allowed)</font>
   * @return Sample Median.
   */
  public static double getSampleMedian(List<Double> data) {
    if (data.size() < 1) {
      throw new IllegalArgumentException(NO_DATA);
    }

    final int midPos = data.size() / 2;

    List<Double> sortedData = data.stream()
        .sorted(Double::compare)
        .collect(Collectors.toList());

    if (data.size() % 2 == 0) {
      return (sortedData.get(midPos - 1) + sortedData.get(midPos)) / 2;
    }
    return sortedData.get(midPos);
  }

  /**
   * Calculate Sample Variance of data.
   *
   * @param data sample data set <font color=orange>(size should larger than 2)</font>
   * @param sampleMean sample Mean of data
   * @return Sample Variance.
   */
  public static double getSampleVariance(List<Double> data, double sampleMean) {
    if (data.size() < 2) {
      throw new IllegalArgumentException(NO_DATA);
    }
    final double sum = data.stream()
        .map(e -> Math.pow(e - sampleMean, 2))
        .reduce(0.0, Double::sum);
    return sum / (data.size() - 1);
  }

  /**
   * Calculate Sample Standard Deviation of data.
   *
   * @param data sample data set
   * @param sampleMean sample Mean of data
   * @return Sample Standard Deviation.
   */
  public static double getSampleStdDev(List<Double> data, double sampleMean) {
    return Math.sqrt(getSampleVariance(data, sampleMean));
  }

  /**
   * Estimate mean by student-t Distribution.
   *
   * @param sampleMean sample Mean of data
   * @param sampleStdDev sample Standard Deviation of data
   * @param dataSize size of data <font color=orange>(should larger than 1)</font>
   * @param confidentLevel Confidential Level
   *                       (<font color=orange>Range: [0, 1]</font>,
   *                       Recommended: 0.01(99%), 0.05(95%), 0.1(90%))
   * @return Mean Range(min, max)
   */
  public static MeanRange estimateMean(double sampleMean, double sampleStdDev, int dataSize,
                                       double confidentLevel) {
    if (dataSize <= 1) {
      throw new IllegalArgumentException(NOT_ENOUGH_DATA_1);
    }

    if (confidentLevel < 0.0 || confidentLevel > 1.0) {
      throw new IllegalArgumentException(LEVEL_OUT_OF_RANGE);
    }

    TDistribution dist = new TDistribution(dataSize - 1);
    final double scoreT = dist.inverseCumulativeProbability(confidentLevel / 2);
    final double error = Math.abs(scoreT * sampleStdDev / Math.sqrt(dataSize));

    return MeanRange.newBuilder()
        .setMin(sampleMean - error)
        .setMax(sampleMean + error)
        .build();
  }

  /**
   * Calculate which GcEvents are out-ranged from median(detects only upper outliers).
   * <a href="http://www.itl.nist.gov/div898/handbook/eda/section3/eda35h1.htm">
   * See how calculated</a>
   *
   * @param data sample data
   * @param sampleMean sample Mean of data
   * @param sampleStdDev sample Standard Deviation of data
   * @param dataSize size of data <font color=orange>(should larger than 2)</font>
   * @param level Confidential Level
   *              (<font color=orange>Range: [0, 1]</font>,
   *              Recommended: 0.01(99%), 0.1(90%), 0.25(75%))
   * @param extractor function to get value
   * @param <T> Type of data
   * @return ArrayList of outlier
   */
  public static <T> ArrayList<T> getOutliers(List<T> data, double sampleMean, double sampleStdDev,
                                             int dataSize, double level, Extractor<T> extractor) {
    if (dataSize <= 2) {
      throw new IllegalArgumentException(NOT_ENOUGH_DATA_2);
    }
    if (level < 0 || level > 1) {
      throw new IllegalArgumentException(LEVEL_OUT_OF_RANGE);
    }

    TDistribution dist = new TDistribution(dataSize - 2);
    final double scoreT = dist.inverseCumulativeProbability((1 - (1 - level)) / dataSize);
    double grubValue = ((dataSize - 1) / Math.sqrt(dataSize))
        * Math.sqrt(Math.pow(scoreT, 2) / (dataSize - 2 + Math.pow(scoreT, 2)));

    return data.stream()
        .filter(e -> (extractor.getValue(e) - sampleMean) / sampleStdDev > grubValue)
        .collect(Collectors.toCollection(ArrayList<T>::new));
  }
}
