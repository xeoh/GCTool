package edu.kaist.algo.analyzer;

import edu.kaist.algo.model.MeanRange;

import org.apache.commons.math3.distribution.TDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Statistics class can calculating statistical things.
 *
 * <ul>
 *   <li>Calculate Total Sum</li>
 *   <li>Calculate Sample Mean</li>
 *   <li>Calculate Sample Median</li>
 *   <li>Calculate Sample Variance</li>
 *   <li>Estimate Mean</li>
 * </ul>
 */
public class Statistics {
  private static final String NOT_ENOUGH_DATA = "NOT ENOUGH DATA, SHOULD BE MORE THAN ONE.";
  private static final String NO_DATA = "NO DATA TO COMPUTE, THUS RETURNING 0";

  private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

  interface GetValueOperator<T> {
    double getValue(T object);
  }

  /**
   * Calculate Total Sum of given data.
   *
   * @param data set of data
   * @param delegate delegate to get value from data
   * @param <T> type of data
   * @return Total Sum of given data
   */
  public static <T> double getTotalSum(List<T> data, GetValueOperator<T> delegate) {
    double sum = 0;

    for (T t : data) {
      sum += delegate.getValue(t);
    }

    return sum;
  }

  /**
   * Calculate Sample Mean of given data.
   *
   * @param data set of data
   * @param delegate delegate to get value from data
   * @param <T> type of data
   * @return sample mean of given data
   */
  public static <T> double getSampleMean(List<T> data, GetValueOperator<T> delegate) {
    final int dataCount = data.size();
    if (dataCount == 0) {
      logger.debug(NO_DATA);
      return 0;
    }

    return getTotalSum(data, delegate) / dataCount;
  }

  /**
   * Calculate Sample Median of given data.
   *
   * <p>Aware, data set world be sorted after method completed.
   *
   * @param data set of data
   * @param delegate delegate to get value from data
   * @param <T> type of data
   * @return Sample Median of given data
   */
  public static <T> double getSampleMedian(List<T> data, GetValueOperator<T> delegate) {
    final int dataCount = data.size();
    if (dataCount == 0) {
      logger.debug(NO_DATA);
      return 0;
    }

    Collections.sort(data, Comparator.comparing(e -> delegate.getValue(e)));

    final int midPos = dataCount / 2;

    if (dataCount % 2 == 0) {
      return (delegate.getValue(data.get(midPos - 1)) + delegate.getValue(data.get(midPos))) / 2;
    }
    return delegate.getValue(data.get(midPos));
  }

  /**
   * Calculate Sample Variance of given data.
   *
   * @param data set of data
   * @param delegate delegate to get value from data
   * @param <T> type of data
   * @return Sample Variance of given data
   */
  public static <T> double getSampleVariance(List<T> data, GetValueOperator<T> delegate) {
    final int dataCount = data.size();
    if (dataCount == 0) {
      logger.debug(NO_DATA);
      return 0;
    }

    double sum = 0;
    final double sampleMean = getSampleMean(data, delegate);

    for (T t : data) {
      sum += (delegate.getValue(t) - sampleMean) * (delegate.getValue(t) - sampleMean);
    }

    return sum / (dataCount - 1);
  }

  /**
   * Estimate mean by student-t Distribution.
   *
   * @param sampleMean Sample Mean
   * @param sampleVariance Sample Variance
   * @param dataCount number of data (will be use for degree of freedom)
   * @param confidentLevel Confidential Level
   *                       (Range: 0 ~ 1, Recommended: 0.01(99%), 0.05(95%), 0.1(90%))
   * @return Mean Range(min, max)
   */
  public static MeanRange estimateMean(double sampleMean, double sampleVariance, int dataCount,
                                       double confidentLevel) {
    if (dataCount <= 1) {
      logger.warn(NOT_ENOUGH_DATA);
      return MeanRange.newBuilder()
          .setMin(Double.NaN)
          .setMax(Double.NaN)
          .build();
    }

    TDistribution dist = new TDistribution(dataCount - 1);
    final double scoreT = dist.inverseCumulativeProbability((1 - (1 - confidentLevel)) / 2);
    final double error = Math.abs(scoreT * Math.sqrt(sampleVariance) / Math.sqrt(dataCount));

    return MeanRange.newBuilder()
        .setMin(sampleMean - error)
        .setMax(sampleMean + error)
        .build();
  }
}
