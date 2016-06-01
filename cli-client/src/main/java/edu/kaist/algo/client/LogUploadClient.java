package edu.kaist.algo.client;

import static com.google.protobuf.ByteString.readFrom;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;

import edu.kaist.algo.service.FileInfo;
import edu.kaist.algo.service.FileUploadResult;
import edu.kaist.algo.service.LogUploadGrpc;
import edu.kaist.algo.service.UploadRequest;
import edu.kaist.algo.service.UploadResult;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A log file uploader for the client.
 */
public class LogUploadClient {
  private static final Logger logger = LoggerFactory.getLogger(LogUploadClient.class);

  private final ManagedChannel channel;
  private final LogUploadGrpc.LogUploadStub asyncStub;
  private final LogUploadGrpc.LogUploadBlockingStub blockingStub;
  private static final int BUFFER_SIZE = 1024;

  /**
   * Construct client to server at {@code host:port}.
   *
   * @param host The host name or IP address to bind to.
   * @param port The port number of the host.
   */
  public LogUploadClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build();
    // a streaming stubs
    asyncStub = LogUploadGrpc.newStub(channel);
    blockingStub = LogUploadGrpc.newBlockingStub(channel);
  }


  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Creates a responseObserver that can be used in the server,
   * and uses an uploadObserver to stream file contents to the server until it is empty.
   * When the upload is done, the server sends the results as UploadResult class,
   * which responseObserver uses to print out the upload summary.
   * @param inputstream File input stream to read in contents of file.
   * @throws InterruptedException Thrown when upload stream is interrupted.
   */
  public void logUpload(FileInputStream inputstream) throws InterruptedException {
    System.out.println("***Upload logfile***");

    StreamObserver<UploadResult> responseObserver = new StreamObserver<UploadResult>() {
      @Override
      public void onNext(UploadResult result) {
        System.out.println("***Upload Result Summary***");
        System.out.println("Successful : " + result.getSuccessful());
        System.out.println("File ID : " + result.getId());
        System.out.println("Total Received File Size : " + result.getFilesize());
      }

      @Override
      public void onError(Throwable thrown) {
        Status status = Status.fromThrowable(thrown);
        System.err.println("***Log Upload Failed" + thrown.getMessage());
      }

      @Override
      public void onCompleted() {
        System.out.println("***Finished logfile upload***");
      }
    };

    StreamObserver<UploadRequest> uploadObserver = asyncStub.logUpload(responseObserver);

    try {
      ByteString bytestring;

      // send file contents until the file is empty
      while (true) {
        bytestring = readFrom(inputstream, BUFFER_SIZE);
        UploadRequest uploadrequest = UploadRequest.newBuilder()
            .setContents(bytestring).build();
        uploadObserver.onNext(uploadrequest);

        // show progress
        System.out.println(bytestring.size() + " bytes uploaded...");

        if (bytestring.isEmpty()) {
          break;
        }
      }
    } catch (RuntimeException re) {
      uploadObserver.onError(re);
      throw re;
    } catch (IOException ie) {
      uploadObserver.onError(ie);
    }

    // upload finished
    uploadObserver.onCompleted();
  }

  /**
   * Send meta-information about the file to be uploaded.
   * @param filename The file name to upload.
   */
  public void infoUpload(String filename) {
    FileInfo fileinfo = FileInfo.newBuilder().setFilename(filename).build();

    try {
      FileUploadResult result = blockingStub.infoUpload(fileinfo);
      System.out.println("File to write on opened on server : " + result.getSuccessful());
    } catch (StatusRuntimeException event) {
      logger.error("infoUpload failed : ", event.getStatus());
    }
  }

  /**
   * The ParsedOptions class contains the parsed command-line options.
   */
  @VisibleForTesting
  static class ParsedOptions {
    private final int port;
    private final String filename;
    private final String host;

    ParsedOptions(String host, String filename, int port) {
      this.host = host;
      this.filename = filename;
      this.port = port;
    }

    int getPort() {
      return port;
    }

    String getHost() {
      return host;
    }

    String getFilename() {
      return filename;
    }
  }

  /**
   * Specify and describe needed options for client's command-line input.
   * @return Options class that contains the option information.
   */
  @VisibleForTesting
  static Options makeOptions() {
    Options options = new Options();
    Option host = Option.builder("h")
        .hasArg(true)
        .argName("host")
        .desc("give the host name or IP address")
        .build();
    Option port = Option.builder("p")
        .hasArg(true)
        .argName("port")
        .desc("give the port number")
        .required()
        .build();
    Option filename = Option.builder("f")
        .hasArg(true)
        .argName("filename")
        .desc("give the absolute / relative path of the file to upload")
        .required()
        .build();
    options.addOption(host);
    options.addOption(port);
    options.addOption(filename);

    return options;
  }

  /**
   * Parses the options in ARGS according to OPTIONS.
   *
   * @param options Contains specification of wanted options.
   * @param args Command-line given.
   * @return Returns a ParsedOptions that contains the results,
   *     or returns null if improper information is given.
   */
  @VisibleForTesting
  static ParsedOptions parseOptions(Options options, String[] args) {
    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine cmd = parser.parse(options, args);

      int port;
      try {
        port = Integer.parseInt(cmd.getOptionValue("p"));
      } catch (NumberFormatException nfe) {
        System.err.println("Must give a number to option 'port'.");
        return null;
      }
      // if given improper port number
      if (port < 0 || port > 65535) {
        System.err.println("Must give a proper port number between 0 and 65535.");
        return null;
      }

      // if given improper file name
      String filename = cmd.getOptionValue("f");
      if (filename.isEmpty()) {
        System.err.println("Must give a proper existing file name.");
        return null;
      }

      // if given improper host
      String host = cmd.getOptionValue("h", "localhost");
      if (host.isEmpty()) {
        System.err.println("Must give a proper host name!");
        return null;
      }
      return new ParsedOptions(host, filename, port);
    } catch (ParseException pe) {
      System.err.println("Parsing failed. Reason : " + pe.getMessage());
    }
    return null;
  }

  /**
   * Looks for the file given the filepath.
   * @param filepath the path of the file to upload.
   * @return Returns the File class. Returns null if the file doesn't exist.
   */
  @VisibleForTesting
  static File lookForFile(String filepath) {
    File file = new File(filepath);

    return file.exists() ? file : null;
  }

  /**
   * Main function of LogUploadClient, which opens an asynchronous gRPC channel
   * send a local file to the server through streaming. The user should give the port number
   * and filename to upload, and, optionally, a host name or IP address.
   * @param args Command-line input.
   */
  public static void main(String[] args) {
    // parse command-line options
    final Options options = makeOptions();
    final ParsedOptions parsedopt = parseOptions(options, args);
    if (parsedopt == null) {
      System.out.println("Client cannot start due to parsing failure.");
      return;
    }

    // bind client to server
    LogUploadClient uploadclient = new LogUploadClient(parsedopt.getHost(), parsedopt.getPort());
    System.out.println("***Client now bound to server.");

    // look for the file to upload
    File logfile = lookForFile(parsedopt.getFilename());
    if (logfile == null) {
      System.out.println("File does not exist!");
      return;
    }

    // give the filename to server to open on server
    uploadclient.infoUpload(parsedopt.getFilename());

    // Send and upload the file
    try {
      FileInputStream inputstream =
          FileUtils.openInputStream(logfile);
      uploadclient.logUpload(inputstream);
    } catch (InterruptedException ie) {
      System.out.println("File upload failed due to interruption.");
    } catch (IOException ioe) {
      System.out.println("Could not open input stream.");
    }

    // shutdown the client
    try {
      uploadclient.shutdown();
    } catch (InterruptedException ie) {
      System.out.println("Client ended during shutdown");
      System.exit(-1);
    }
  }
}
