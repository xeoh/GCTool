package edu.kaist.algo.analyzer;

import edu.kaist.algo.model.MeanRange;

import org.apache.commons.math3.distribution.TDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;
import java.util.Collections;
import java.util.Comparator;
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
public class Statistics<T> {
  private static final String NOT_ENOUGH_DATA_1 = "NOT ENOUGH DATA, SHOULD BE MORE THAN ONE.";
  private static final String NOT_ENOUGH_DATA_2 = "NOT ENOUGH DATA, SHOULD BE MORE THAN TWO.";
  private static final String LEVEL_OUT_OF_RANGE = "LEVEL IS OUT OF RANGE.";
  private static final String NO_DATA = "NO DATA TO COMPUTE EVERY VALUE WILL BE ZERO";

  private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

  interface GetValueOperator<T> {
    double getValue(T object);
  }

  private List<T> data;
  private GetValueOperator<T> delegate;

  private boolean totalAndMeanCalculated = false;
  private boolean varianceCalculated = false;
  private boolean medianCalculated = false;

  private double total = 0;
  private double sampleMean = 0;
  private double sampleVariance = 0;
  private double sampleMedian = 0;

  /**
   * Constructor of Statistics.
   *
   * <p><font color=orange>Aware, this copy list elements.
   * Need careful carefully use for unnecessary memory usage. </font>
   *
   * @param data data to calculate
   * @param delegate delegate to get value
   */
  public Statistics(List<T> data, GetValueOperator<T> delegate) {
    this.data = data.stream().collect(Collectors.toList());
    this.delegate = delegate;

    if (data.size() == 0) {
      logger.warn(NO_DATA);

      totalAndMeanCalculated = true;
      varianceCalculated = true;
      medianCalculated = true;
    } else {
      Collections.sort(data, Comparator.comparing(e -> delegate.getValue(e)));
    }
  }

  private void calculatedTotalAndSampleMean() {
    double sum = 0;
    for (T t : data) {
      sum += delegate.getValue(t);
    }

    this.total = sum;
    this.sampleMean = sum / data.size();

    totalAndMeanCalculated = true;
  }

  /**
   * Get data size
   *
   * @return size of data.
   */
  public int getSize() {
    return data.size();
  }

  /**
   * Get data which has min value.
   *
   * @return minimum valued data
   */
  public T getMin() {
    if (getSize() == 0) {
      logger.warn(NO_DATA);
      return null;
    }

    if (!totalAndMeanCalculated) {
      // if not median calculated, data is not sorted
      calculatedTotalAndSampleMean();
    }
    return data.get(0);
  }

  /**
   * Get data which has max value.
   *
   * @return maximum valued data
   */
  public T getMax() {
    if (getSize() == 0) {
      logger.warn(NO_DATA);
      return null;
    }

    if (!totalAndMeanCalculated) {
      // if not median calculated, data is not sorted
      calculatedTotalAndSampleMean();
    }
    return data.get(getSize() - 1);
  }

  /**
   * Calculate Total Sum of data.
   *
   * @return Total Sum.
   */
  public double getTotalSum() {
    if (!totalAndMeanCalculated) {
      calculatedTotalAndSampleMean();
    }
    return total;
  }

  /**
   * Calculate Sample Mean of data.
   *
   * @return Sample Mean.
   */
  public double getSampleMean() {
    if (!totalAndMeanCalculated) {
      calculatedTotalAndSampleMean();
    }
    return sampleMean;
  }

  /**
   * Calculate Sample Median of data.
   *
   * @return Sample Median.
   */
  public double getSampleMedian() {
    if (!medianCalculated) {
      final int midPos = data.size() / 2;

      if (data.size() % 2 == 0) {
        sampleMedian = (delegate.getValue(data.get(midPos - 1))
            + delegate.getValue(data.get(midPos))) / 2;
      } else {
        sampleMedian = delegate.getValue(data.get(midPos));
      }

      medianCalculated = true;
    }
    return sampleMedian;
  }

  /**
   * Calculate Sample Variance of data.
   *
   * @return Sample Variance.
   */
  public double getSampleVariance() {
    if (!varianceCalculated) {
      double sum = 0;
      for (T t : data) {
        sum += Math.pow(delegate.getValue(t) - getSampleMean(), 2);
      }
      sampleVariance = sum / (data.size() - 1);

      varianceCalculated = true;
    }
    return sampleVariance;
  }

  public double getSampleStdDev() {
    return Math.sqrt(getSampleVariance());
  }

  /**
   * Estimate mean by student-t Distribution.
   *
   * @param confidentLevel Confidential Level
   *                       (Range: 0 ~ 1, Recommended: 0.01(99%), 0.05(95%), 0.1(90%))
   * @return Mean Range(min, max)
   */
  public MeanRange estimateMean(double confidentLevel) {
    if (data.size() <= 1) {
      logger.warn(NOT_ENOUGH_DATA_1);
      return MeanRange.newBuilder()
          .setMin(Double.NaN)
          .setMax(Double.NaN)
          .build();
    }

    if (confidentLevel < 0 || confidentLevel > 1) {
      logger.warn(LEVEL_OUT_OF_RANGE);
      return MeanRange.newBuilder()
          .setMin(Double.NaN)
          .setMax(Double.NaN)
          .build();
    }

    TDistribution dist = new TDistribution(getSize() - 1);
    final double scoreT = dist.inverseCumulativeProbability((1 - (1 - confidentLevel)) / 2);
    final double error = Math.abs(scoreT * getSampleStdDev() / Math.sqrt(getSize()));

    return MeanRange.newBuilder()
        .setMin(getSampleMean() - error)
        .setMax(getSampleMean() + error)
        .build();
  }

  /**
   * Calculate which GcEvents are out-ranged from median(detects only upper outliers).
   *     <a href="http://www.itl.nist.gov/div898/handbook/eda/section3/eda35h1.htm">
   *     See how calculated</a>
   *
   * @param out list to contain output.
   *            <font color=orange>Aware, this method will simply add results to this list.</font>
   * @param level significant level
   *              (Range: 0 ~ 1, Recommended: 0.01(99%), 0.1(90%), 0.25(75%)
   * @return outliers
   */
  public List<T> getOutliers(List<T> out, double level) {
    if (getSize() < 3) {
      logger.warn(NOT_ENOUGH_DATA_2);
      return out;
    }
    if (level < 0 || level > 1) {
      logger.warn(LEVEL_OUT_OF_RANGE);
      return out;
    }

    TDistribution dist = new TDistribution(getSize() - 2);
    final double scoreT = dist.inverseCumulativeProbability((1 - (1 - level)) / getSize());
    double grubbValue = ((getSize() - 1) / Math.sqrt(getSize()))
        * Math.sqrt(Math.pow(scoreT, 2) / (getSize() - 2 + Math.pow(scoreT, 2)));

    for (T t : data) {
      double testValue = (delegate.getValue(t) - getSampleMean()) / getSampleStdDev();
      if (testValue > grubbValue) {
        out.add(t);
      }
    }

    return out;
  }
}
