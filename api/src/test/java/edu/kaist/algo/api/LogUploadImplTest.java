/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.client.AnalysisDataRequester;
import edu.kaist.algo.client.LogUploader;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.fiftyonred.mock_jedis.MockJedisPool;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import edu.kaist.algo.service.AnalysisStatus;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * The server receives the file from the client and tests the validity of
 * file contents.
 */
@RunWith(JUnit4.class)
public class LogUploadImplTest {
  private File uploadFile;
  private static final String UPLOADED_FILE_NAME = "uploaded.log";
  private static final String RESOURCE_FILE_NAME = "hotspot_pid6017.log";
  private static final int TEST_PORT = 50053;

  // open a sample real GC log file
  @Rule
  public ResourceFile resourceFile = new ResourceFile(RESOURCE_FILE_NAME);
  GcToolServer server;
  private Ticketer ticketer;

  /**
   * Set up the server and client to use in the test.
   */
  @Before
  public void setUp() {
    JedisPool jedisPool = new MockJedisPool(new JedisPoolConfig(), "localhost");
    ticketer = new Ticketer(jedisPool);
    server = new GcToolServer(TEST_PORT, jedisPool);

    // start the server
    try {
      server.start();
    } catch (IOException ie) {
      ie.printStackTrace();
      System.err.println("LogUploadClientTest : server failed to start.");
    }

    // indicate the test logfile
    uploadFile = resourceFile.getFile();
  }

  /**
   * ResourceFile class that opens an existing file from resources folder
   * for testing purposes. Extends ExternalResource class of @Rule.
   */
  public class ResourceFile extends ExternalResource {
    URL url;
    String filepath;
    FileInputStream inputstream;
    File file;

    ResourceFile(String filename) {
      this.filepath = filename;
      this.url = getClass().getClassLoader().getResource(filename);
    }

    @Override
    protected void before() throws Throwable {
      file = new File(url.getFile());
      try {
        inputstream = FileUtils.openInputStream(file);
      } catch (FileNotFoundException fnfe) {
        inputstream = null;
      }
    }

    @Override
    protected void after() {
      try {
        inputstream.close();
      } catch (IOException ie) {
        System.err.println("LogUploadClientTest : closing input stream error.");
      }
    }

    FileInputStream getInputstream() {
      return inputstream;
    }

    File getFile() {
      return file;
    }
  }

  /**
   * Tests the correctness of log file uploading.
   */
  @Test
  public void logUploadServerFileContentsTest() {
    // assert that upload resource file exists
    assertTrue(uploadFile.exists());

    // open the uploader and transmit the resource file before testing
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", TEST_PORT)
        .usePlaintext(true)
        .build();

    LogUploader logUploader = new LogUploader(channel);
    try {
      long ticket = logUploader.uploadInfo(UPLOADED_FILE_NAME);
      logUploader.uploadLog(ticket, resourceFile.getInputstream());
    } catch (InterruptedException ie) {
      System.err.println("Interrupted during upload!");
    }

    try {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      fail("Client ended during shutdown");
    }

    // open the result file and assert their existence
    File resultfile = new File(UPLOADED_FILE_NAME);
    assertTrue(resultfile.exists());

    // compare the contents of resulting file and target file
    try {
      System.out.println(uploadFile.toString());
      System.out.println(resultfile.toString());
      assertTrue(FileUtils.contentEquals(uploadFile, resultfile));
    } catch (IOException io) {
      fail("File contents does not match!");
    }

    // compare the size
    assertEquals(FileUtils.sizeOf(uploadFile), FileUtils.sizeOf(resultfile));
  }

  @Test
  public void testTriggerLogAnalyzeJob() throws Exception {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", TEST_PORT)
        .usePlaintext(true)
        .build();

    // Upload log file, and wait for analyzing.
    LogUploader logUploader = new LogUploader(channel);
    long ticket = logUploader.uploadInfo(UPLOADED_FILE_NAME);
    logUploader.uploadLog(ticket, resourceFile.getInputstream());

    AnalysisStatus status;
    while ((status = ticketer.getStatus(ticket)) != AnalysisStatus.COMPLETED) {
      assertTrue(EnumSet.of(AnalysisStatus.NOT_READY, AnalysisStatus.ANALYZING).contains(status));
      Thread.sleep(100);
    }

    AnalysisDataRequester requester = new AnalysisDataRequester(channel);
    GcAnalyzedData result = requester.requestAnalysisData(ticket);
    GcAnalyzedData resultFromResult = GcTestUtils.parseFromResource(RESOURCE_FILE_NAME);
    assertNotNull(result);
    assertEquals(resultFromResult, result);

    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Delete the resulting file after testing is completed.
   */
  @After
  public void cleanUp() {
    File resultfile = new File(UPLOADED_FILE_NAME);
    resultfile.deleteOnExit();

    server.stop();
  }
}
