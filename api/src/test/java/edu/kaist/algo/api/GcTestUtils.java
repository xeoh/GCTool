/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.api;

import com.google.common.io.Resources;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analyzer.LogAnalyzer;
import edu.kaist.algo.model.GcEvent;
import edu.kaist.algo.parser.CmsLogParser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Helper functions for testing.
 */
public class GcTestUtils {

  /**
   * Returns GcAnalyzedData from the resource file.
   * @param resource the name of resource file
   * @return the parsed GcAnalyzedData
   * @throws URISyntaxException if the uri is malformed.
   */
  public static GcAnalyzedData parseFromResource(String resource) throws URISyntaxException {
    Path path = Paths.get(Resources.getResource(resource).toURI());
    CmsLogParser parser = new CmsLogParser();
    List<GcEvent> events = parser.parse(path);
    LogAnalyzer analyzer = new LogAnalyzer(events);
    return analyzer.analyzeData();
  }
}
