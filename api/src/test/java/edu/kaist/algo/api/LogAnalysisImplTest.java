package edu.kaist.algo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.io.Resources;

import com.fiftyonred.mock_jedis.MockJedisPool;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.analyzer.LogAnalyzer;
import edu.kaist.algo.client.AnalysisDataRequester;
import edu.kaist.algo.model.GcEvent;
import edu.kaist.algo.parser.CmsLogParser;
import edu.kaist.algo.service.AnalysisStatus;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RunWith(JUnit4.class)
public class LogAnalysisImplTest {
  private static final int TEST_PORT = 50053;
  private static final long NOT_READY_TICKET = 1001;
  private static final long COMPLETED_TICKET = 1002;
  private static final long ANALYZING_TICKET = 1003;
  private static final long ERROR_TICKET = 1004;
  private static final String RESOURCE_FILE_NAME = "hotspot_long.log";
  private static final String ANALYZED_FILE_NAME = "data";

  @Rule public AnalyzedFile analyzedFile;
  private GcToolServer server;

  public class AnalyzedFile extends ExternalResource {
    public String filepath;
    public File file;
    public GcAnalyzedData data;

    AnalyzedFile(String srcname, String filename) throws Exception {
      this.file = File.createTempFile(filename, filename);
      this.filepath = file.getAbsolutePath();

      CmsLogParser parser = new CmsLogParser();
      final Path path = Paths.get(Resources.getResource(srcname).toURI());
      List<GcEvent> events = parser.parse(path);
      LogAnalyzer logAnalyzer = new LogAnalyzer(events);
      data = logAnalyzer.analyzeData();

      OutputStream outputStream = FileUtils.openOutputStream(file);
      data.writeTo(outputStream);
      outputStream.close();
    }

    @Override
    protected void after() {
      file.deleteOnExit();
    }
  }

  /**
   * set up for testing.
   *
   * @throws Exception file system fail
   */
  @Before
  public void setUp() throws Exception {
    JedisPool jedisPool = new MockJedisPool(new JedisPoolConfig(), "localhost");
    Ticketer ticketer = new Ticketer(jedisPool);
    server = new GcToolServer(TEST_PORT, jedisPool);

    try {
      server.start();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.out.println("LogAnalysisImplTest : server failed to start.");
    }

    analyzedFile = new AnalyzedFile(RESOURCE_FILE_NAME, ANALYZED_FILE_NAME);

    ticketer.setStatus(NOT_READY_TICKET, AnalysisStatus.NOT_READY);
    ticketer.setStatus(COMPLETED_TICKET, AnalysisStatus.COMPLETED);
    ticketer.setStatus(ANALYZING_TICKET, AnalysisStatus.ANALYZING);
    ticketer.setStatus(ERROR_TICKET, AnalysisStatus.ERROR);

    ticketer.setResult(COMPLETED_TICKET, analyzedFile.filepath);
  }

  /**
   * clean up after testing.
   */
  @After
  public void cleanUp() {
    server.stop();
  }

  @Test
  public void testLogAnalysisRequest_ReturnAnalyzedData() {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", TEST_PORT)
        .usePlaintext(true)
        .build();

    AnalysisDataRequester requester = new AnalysisDataRequester(channel);

    GcAnalyzedData notReadyData = requester.requestAnalysisData(NOT_READY_TICKET);
    assertNull(notReadyData);

    GcAnalyzedData analyzingData = requester.requestAnalysisData(ANALYZING_TICKET);
    assertNull(analyzingData);

    GcAnalyzedData errorData = requester.requestAnalysisData(ERROR_TICKET);
    assertNull(errorData);

    GcAnalyzedData completedData = requester.requestAnalysisData(COMPLETED_TICKET);
    assertEquals(analyzedFile.data.toString(), completedData.toString());
  }
}
