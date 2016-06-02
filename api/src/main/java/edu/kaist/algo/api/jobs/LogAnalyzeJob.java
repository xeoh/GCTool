package edu.kaist.algo.api.jobs;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analyzer.LogAnalyzer;
import edu.kaist.algo.api.Ticketer;
import edu.kaist.algo.model.GcEvent;
import edu.kaist.algo.parser.CmsLogParser;
import edu.kaist.algo.service.AnalysisStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

/**
 * Log analyzing job for the background work.
 */
public class LogAnalyzeJob implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(LogAnalyzeJob.class);

  private final Ticketer ticketer;

  private final long ticket;

  /**
   * Return the LogAnalyzedJob for the given ticket number.
   * @param ticketer the ticketer instance
   * @param ticket the ticket number
   */
  public LogAnalyzeJob(Ticketer ticketer, long ticket) {
    this.ticketer = ticketer;
    this.ticket = ticket;
  }

  @Override
  public void run() {
    ticketer.setStatus(ticket, AnalysisStatus.ANALYZING);
    final CmsLogParser parser = new CmsLogParser();
    try {
      final List<GcEvent> parsedResult = parser.parse(Paths.get(ticketer.getLogFile(ticket)));
      final LogAnalyzer analyzer = new LogAnalyzer(parsedResult);
      final GcAnalyzedData result = analyzer.analyzeData();
      ticketer.setResult(ticket, result);
      ticketer.setStatus(ticket, AnalysisStatus.COMPLETED);
    } catch (Exception e) {
      ticketer.setStatus(ticket, AnalysisStatus.ERROR);
      logger.error("Failed to analyze the log.", e);
    }
  }
}
