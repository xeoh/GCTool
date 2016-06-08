/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.api.jobs;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analyzer.LogAnalyzer;
import edu.kaist.algo.model.GcEvent;
import edu.kaist.algo.parser.CmsLogParser;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.nio.file.Paths;
import java.util.List;

public class LogAnalyzingJob implements Job {
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    final JobDataMap data = context.getJobDetail().getJobDataMap();
    final String logFilePath = data.getString("logFilePath");

    final CmsLogParser parser = new CmsLogParser();
    final List<GcEvent> parsedResult =  parser.parse(Paths.get(logFilePath));

    final LogAnalyzer analyzer = new LogAnalyzer(parsedResult);
    final GcAnalyzedData result = analyzer.analyzeData();


  }
}
