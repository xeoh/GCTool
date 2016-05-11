package edu.kaist.algo.analyzer;

import static org.junit.Assert.assertEquals;

import edu.kaist.algo.model.MeanRange;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

/**
 * Unit Testing Statistics.java.
 */
@RunWith(JUnit4.class)
public class StatisticsTest {
  /**
   * Test Class containing tow value.
   */
  public class TwoValue {
    public int v1;
    public double v2;

    /**
     * Constructor of TwoValue class.
     *
     * @param p1 first value
     * @param p2 second value
     */
    public TwoValue(int p1, float p2) {
      v1 = p1;
      v2 = p2;
    }
  }

  Statistics.GetValueOperator<TwoValue> getV1 = t -> (double)t.v1;
  Statistics.GetValueOperator<TwoValue> getV2 = t -> (double)t.v2;

  ArrayList<TwoValue> testSetEven;
  ArrayList<TwoValue> testSetOdd;
  ArrayList<TwoValue> testSetNone;

  @Before public void setUp() {
    testSetEven = new ArrayList<>();
    testSetOdd = new ArrayList<>();
    testSetNone = new ArrayList<>();

    TwoValue e1 = new TwoValue(1,2);
    TwoValue e2 = new TwoValue(2,3);
    TwoValue e3 = new TwoValue(3,4);
    TwoValue e4 = new TwoValue(4,5);
    TwoValue e5 = new TwoValue(3,5);

    testSetEven.add(e1);
    testSetEven.add(e2);
    testSetEven.add(e3);
    testSetEven.add(e4);

    testSetOdd.add(e1);
    testSetOdd.add(e2);
    testSetOdd.add(e3);
    testSetOdd.add(e4);
    testSetOdd.add(e5);
  }

  @Test public void testGetTotalSum_ReturnTotalSum() {
    double totalSumV1 = Statistics.getTotalSum(testSetEven, getV1);
    assertEquals(10, totalSumV1, 0.0001);

    double totalSumV2 = Statistics.getTotalSum(testSetEven, getV2);
    assertEquals(14, totalSumV2, 0.0001);

    double totalSumNone = Statistics.getTotalSum(testSetNone, getV1);
    assertEquals(0, totalSumNone, 0.0001);
  }

  @Test public void testGetSampleMean_ReturnSampleMean() {
    double sampleMeanV1 = Statistics.getSampleMean(testSetEven, getV1);
    assertEquals(10f / 4, sampleMeanV1, 0.0001);

    double sampleMeanV2 = Statistics.getSampleMean(testSetEven, getV2);
    assertEquals(14f / 4, sampleMeanV2, 0.0001);

    double sampleMeanNone = Statistics.getSampleMean(testSetNone, getV1);
    assertEquals(0, sampleMeanNone, 0.0001);
  }

  @Test public void testGetSampleMedian_ReturnSampleMedian() {
    double sampleMedianV1 = Statistics.getSampleMedian(testSetEven, getV1);
    assertEquals((2f + 3f) / 2, sampleMedianV1, 0.0001);

    double sampleMedianV2 = Statistics.getSampleMedian(testSetOdd, getV2);
    assertEquals(4, sampleMedianV2, 0.0001);

    double sampleMedianNone = Statistics.getSampleMedian(testSetNone, getV1);
    assertEquals(0, sampleMedianNone, 0.0001);
  }

  @Test public void testGetSampleVariance_ReturnSampleVariance() {
    double sampleVarianceV1 = Statistics.getSampleVariance(testSetEven, getV1);
    assertEquals(1.66667, sampleVarianceV1, 0.0001);

    double sampleVarianceV2 = Statistics.getSampleVariance(testSetEven, getV2);
    assertEquals(1.66667, sampleVarianceV2, 0.0001);

    double sampleVarianceNone = Statistics.getSampleVariance(testSetNone, getV1);
    assertEquals(0, sampleVarianceNone, 0.0001);
  }

  @Test public void testEstimateMean_returnMeanRange() {
    // Observed value for v1 of testSetEven
    // sample mean = 2.5
    // sample variance = 1.66667, sample standard deviation = 1.29099
    double sampleMeanV1 = Statistics.getSampleMean(testSetEven, getV1);
    double sampleVarianceV1 = Statistics.getSampleVariance(testSetEven, getV1);

    // Calculated on "http://www.danielsoper.com/statcalc/calculator.aspx?id=96"
    // Estimating 99% confidence interval
    MeanRange meanRange99 = Statistics.estimateMean(sampleMeanV1, sampleVarianceV1,
        testSetEven.size(), 0.01);
    assertEquals(-1.27028, meanRange99.getMin(), 0.0001);
    assertEquals(6.27028, meanRange99.getMax(), 0.0001);

    // Estimating 95% confidence interval
    MeanRange meanRange95 = Statistics.estimateMean(sampleMeanV1, sampleVarianceV1,
        testSetEven.size(), 0.05);
    assertEquals(0.44575, meanRange95.getMin(), 0.0001);
    assertEquals(4.55425, meanRange95.getMax(), 0.0001);

    // Estimating 90% confidence interval
    MeanRange meanRange90 = Statistics.estimateMean(sampleMeanV1, sampleVarianceV1,
        testSetEven.size(), 0.1);
    assertEquals(0.98092, meanRange90.getMin(), 0.0001);
    assertEquals(4.01908, meanRange90.getMax(), 0.0001);

    // Testing invalid data
    double sampleMeanNone = Statistics.getSampleMean(testSetNone, getV1);
    double sampleVarianceNone = Statistics.getSampleVariance(testSetNone, getV1);
    MeanRange meanRangeNaN = Statistics.estimateMean(sampleMeanNone, sampleVarianceNone,
        testSetNone.size(), 0.1);
    assertEquals(Double.NaN, meanRangeNaN.getMin(), 0.0001);
    assertEquals(Double.NaN, meanRangeNaN.getMax(), 0.0001);
  }
}
