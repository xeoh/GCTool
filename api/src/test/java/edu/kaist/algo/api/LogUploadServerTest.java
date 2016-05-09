package edu.kaist.algo.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.kaist.algo.client.LogUploadClient;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;


/**
 * The server receives the file from the client and tests the validity of
 * file contents.
 */
@RunWith(JUnit4.class)
public class LogUploadServerTest {
  private LogUploadServer server;
  private LogUploadClient client;
  private File uploadfile;

  @Rule
  public ResourceFile resourceFile = new ResourceFile("hotspot_pid6017.log"); // short sample file

  /**
   * Set up the server and client to use in the test.
   */
  @Before
  public void setUp() {
    server = new LogUploadServer(50051);
    client = new LogUploadClient("localhost", 50051);

    // start the server
    try {
      server.start();
    } catch (IOException ie) {
      ie.printStackTrace();
      System.err.println("LogUploadClientTest : server failed to start.");
    }

    // open client
    client = new LogUploadClient("localhost", 50051);

    // indicate the test logfile
    uploadfile = resourceFile.getFile();
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
   * Tests whether the transferred file via gRPC service has been done correctly.
   */
  @Test
  public void logUploadServerFileContentsTest() {
    // transfer file via gRPC
    try {
      client.logUpload(resourceFile.getInputstream());
    } catch (InterruptedException ie) {
      System.err.println("Interrupted during upload!");
    }
    // wait until the client has done transmitting
    try {
      client.shutdown();
    } catch (InterruptedException ie) {
      System.out.println("Client ended during shutdown");
    }
    // open the result file
    File resultfile = new File("uploaded.log");

    // compare the contents of resulting file and target file
    boolean contentequals;
    try {
      contentequals = FileUtils.contentEquals(uploadfile, resultfile);
    } catch (IOException io) {
      contentequals = false;
    }

    assertTrue(uploadfile.exists());
    assertTrue(resultfile.exists());
    assertTrue(contentequals);
    assertEquals(FileUtils.sizeOf(uploadfile), FileUtils.sizeOf(resultfile));
  }

  /**
   * Delete the resulting file after testing is completed.
   */
  @After
  public void cleanUp() {
    File resultfile = new File("uploaded.log");
    resultfile.deleteOnExit();
  }
}