package edu.kaist.algo.analyzer;

import static org.junit.Assert.assertEquals;

import edu.kaist.algo.model.MeanRange;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Unit Testing Statistics.java.
 */
@RunWith(JUnit4.class)
public class StatisticsTest {
  /**
   * Test Class containing tow value.
   */
  public class TwoValue {
    private int v1;
    private double v2;

    /**
     * Constructor of TwoValue class.
     *
     * @param p1 first value
     * @param p2 second value
     */
    public TwoValue(int p1, double p2) {
      v1 = p1;
      v2 = p2;
    }

    public double getV1() {
      return (double)v1;
    }

    public double getV2() {
      return v2;
    }
  }

  ArrayList<TwoValue> testSetEven;
  ArrayList<TwoValue> testSetOdd;
  ArrayList<TwoValue> testSetNone;

  /**
   * Pre setup for unit test.
   */
  @Before public void setUp() {
    testSetEven = new ArrayList<>();
    testSetOdd = new ArrayList<>();
    testSetNone = new ArrayList<>();

    TwoValue e1 = new TwoValue(1,2);
    TwoValue e2 = new TwoValue(2,3);
    TwoValue e3 = new TwoValue(3,4);
    TwoValue e4 = new TwoValue(4,5);

    testSetEven.add(e1);
    testSetEven.add(e2);
    testSetEven.add(e3);
    testSetEven.add(e4);

    testSetOdd.add(e1);
    testSetOdd.add(e2);
    testSetOdd.add(e3);
    testSetOdd.add(e4);
    testSetOdd.add(new TwoValue(3,5));
  }

  @Test public void testGetTotalSum_ReturnTotalSum() {

    double totalSumV1 = Statistics.getTotalSum(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    assertEquals(10, totalSumV1, 0.0001);

    double totalSumV2 = Statistics.getTotalSum(testSetEven.stream()
        .map(TwoValue::getV2)
        .collect(Collectors.toList()));
    assertEquals(14, totalSumV2, 0.0001);

    double totalSumNone = Statistics.getTotalSum(testSetNone.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    assertEquals(0, totalSumNone, 0.0001);
  }

  @Test public void testGetSampleMean_ReturnSampleMean() {
    double sampleMeanV1 = Statistics.getSampleMean(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    assertEquals(10f / 4, sampleMeanV1, 0.0001);

    double sampleMeanV2 = Statistics.getSampleMean(testSetEven.stream()
        .map(TwoValue::getV2)
        .collect(Collectors.toList()));
    assertEquals(14f / 4, sampleMeanV2, 0.0001);

    double sampleMeanNone = Statistics.getSampleMean(testSetNone.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    assertEquals(0, sampleMeanNone, 0.0001);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSampleMedian_ReturnSampleMedian() {
    double sampleMedianV1 = Statistics.getSampleMedian(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    assertEquals((2f + 3f) / 2, sampleMedianV1, 0.0001);

    double sampleMedianV2 = Statistics.getSampleMedian(testSetOdd.stream()
        .map(TwoValue::getV2)
        .collect(Collectors.toList()));
    assertEquals(4, sampleMedianV2, 0.0001);

    Statistics.getSampleMedian(testSetNone.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
  }

  @Test public void testGetSampleVariance_ReturnSampleVariance() {
    double sampleMeanV1 = Statistics.getSampleMean(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    double sampleVarianceV1 = Statistics.getSampleVariance(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()),
        sampleMeanV1);
    assertEquals(1.66667, sampleVarianceV1, 0.0001);

    double sampleMeanV2 = Statistics.getSampleMean(testSetEven.stream()
        .map(TwoValue::getV2)
        .collect(Collectors.toList()));
    double sampleVarianceV2 = Statistics.getSampleVariance(testSetEven.stream()
        .map(TwoValue::getV2)
        .collect(Collectors.toList()),
        sampleMeanV2);
    assertEquals(1.66667, sampleVarianceV2, 0.0001);
  }

  @Test public void testEstimateMean_returnMeanRange() {
    // Observed value for v1 of testSetEven
    // sample mean = 2.5
    // sample variance = 1.66667, sample standard deviation = 1.29099

    // Calculated on "http://www.danielsoper.com/statcalc/calculator.aspx?id=96"
    // Estimating 99% confidence interval
    double sampleMeanV1 = Statistics.getSampleMean(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()));
    double sampleStdDevV1 = Statistics.getSampleStdDev(testSetEven.stream()
        .map(TwoValue::getV1)
        .collect(Collectors.toList()),
        sampleMeanV1);
    MeanRange meanRange99 = Statistics.estimateMean(sampleMeanV1,
        sampleStdDevV1,
        testSetEven.size(),
        0.01
    );
    assertEquals(-1.27028, meanRange99.getMin(), 0.0001);
    assertEquals(6.27028, meanRange99.getMax(), 0.0001);

    // Estimating 95% confidence interval
    MeanRange meanRange95 = Statistics.estimateMean(sampleMeanV1,
        sampleStdDevV1,
        testSetEven.size(),
        0.05
    );
    assertEquals(0.44575, meanRange95.getMin(), 0.0001);
    assertEquals(4.55425, meanRange95.getMax(), 0.0001);

    // Estimating 90% confidence interval
    MeanRange meanRange90 = Statistics.estimateMean(sampleMeanV1,
        sampleStdDevV1,
        testSetEven.size(),
        0.1
    );
    assertEquals(0.98092, meanRange90.getMin(), 0.0001);
    assertEquals(4.01908, meanRange90.getMax(), 0.0001);
  }

  @Test public void testGetOutliers_returnOutliers() {
    ArrayList<Double> testSet = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      testSet.add(Math.random() * 2 - 1); // range[-1 ~ 1]
    }
    testSet.add(20.0);
    testSet.add(-20.0);

    double sampleMean = Statistics.getSampleMean(testSet);
    double sampleStdDev = Statistics.getSampleStdDev(testSet, sampleMean);
    ArrayList<Double> outliers = Statistics.getOutliers(testSet, sampleMean, sampleStdDev,
        testSet.size(), 0.25, Double::doubleValue);

    assertEquals(outliers.size(), 1);
    assertEquals(outliers.get(0), 20.0, 0.0001);
  }

  @Test public void testMinMax_returnMinMax() {
    assertEquals(1, Statistics.getMin(testSetEven, TwoValue::getV1).getV1(), 0.0001);
    assertEquals(4, Statistics.getMax(testSetEven, TwoValue::getV1).getV1(), 0.0001);
  }
}
