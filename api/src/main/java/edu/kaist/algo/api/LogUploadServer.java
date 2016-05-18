package edu.kaist.algo.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;

import edu.kaist.algo.client.FileInfo;
import edu.kaist.algo.client.FileUploadResult;
import edu.kaist.algo.client.GcServiceGrpc;
import edu.kaist.algo.client.UploadRequest;
import edu.kaist.algo.client.UploadResult;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A logfile receiving server.
 */
public class LogUploadServer {
  private static final Logger logger = LoggerFactory.getLogger(LogUploadServer.class);
  private final int port;
  private final Server server;

  /**
   * Constructs a server at given PORT number.
   * @param port the port number.
   */
  public LogUploadServer(int port) {
    this.port = port;
    this.server = ServerBuilder.forPort(port)
        .addService(GcServiceGrpc.bindService(new GcServiceImpl()))
        .build();
  }

  @VisibleForTesting
  void start() throws IOException {
    server.start();
    logger.info("Server started, listening on " + this.port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** Shutting down gRPC server since JVM is shutting down");
        LogUploadServer.this.stop();
        System.err.println("*** Server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  // wait until the gRPC channel is severed
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Defines a service that receives a stream of logfile and creating a file on
   * the server.
   */
  private static class GcServiceImpl implements GcServiceGrpc.GcService {
    File uploadedFile;

    /**
     * Single-shot gRPC service receiving file information from the client.
     *
     * <p>From the file name, a new file is created. Resulting file
     * is used to write contents received from the client in
     * logUpload() method.
     * @param fileinfo The file information such as file name, date, etc.
     * @param responseObserver StreamObserver type from client.
     */
    @Override
    public void infoUpload(FileInfo fileinfo,
                           StreamObserver<FileUploadResult> responseObserver) {
      responseObserver.onNext(openNewFile(fileinfo.getFilename()));
      responseObserver.onCompleted();
    }

    /**
     * Opens up a new file given by the filename.
     * The file must not exits already.
     * @param filename the filename (full directory name is OK) of a new file.
     * @return The new file opened only by the filename, without the directory.
     */
    private FileUploadResult openNewFile(String filename) {
      uploadedFile = new File(FilenameUtils.getName(filename));
      return FileUploadResult.newBuilder()
          .setSuccessful(uploadedFile.exists()).build();
    }

    /**
     * Receives file contents from the client and writes to file
     * created in infoUpload() method.
     * @param responseObserver StreamObserver type from the client.
     * @return StreamObserver type from the server.
     */
    @Override
    public StreamObserver<UploadRequest> logUpload(
        final StreamObserver<UploadResult> responseObserver) {

      FileOutputStream outputstream;
      try {
        outputstream = FileUtils.openOutputStream(uploadedFile);
      } catch (FileNotFoundException fnfe) {
        logger.error("File cannot be created or cannot be opened.", fnfe);
        return null;
      } catch (IOException ioe) {
        logger.error("Could not open file output stream.", ioe);
        return null;
      }

      return new StreamObserver<UploadRequest>() {
        int totalsize = 0;
        int id = 1; // temporary default number
        private UploadResult result;

        @Override
        public void onNext(UploadRequest uploadrequest) {
          ByteString bytestring = uploadrequest.getContents();
          int size = bytestring.size();
          try {
            bytestring.writeTo(outputstream);
          } catch (IOException ie) {
            logger.error("Error occurred during file receiving : " + ie.getMessage());
          }
          totalsize += size;
        }

        @Override
        public void onError(Throwable thrown) {
          Status status = Status.fromThrowable(thrown);
          logger.error("Log receiving failed : " + status.getDescription());
          result = UploadResult.newBuilder().setFilesize(totalsize)
              .setId(id).setSuccessful(false).build();
          responseObserver.onNext(result);
        }

        @Override
        public void onCompleted() {
          // build Upload Result
          uploadedFile = null;
          result = UploadResult.newBuilder().setFilesize(totalsize)
              .setId(id).setSuccessful(true).build();
          responseObserver.onNext(result);
          responseObserver.onCompleted();
        }
      };
    }
  }

  private static Options makeServerOptions() {
    Options options = new Options();
    Option port = Option.builder("p")
        .hasArg(true)
        .argName("port")
        .desc("give the port number")
        .build();

    options.addOption(port);
    return options;
  }

  private static int parseOptions(Options options, String[] args)
      throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    int port;
    try {
      port = Integer.parseInt(cmd.getOptionValue("p", "50051"));
    } catch (NumberFormatException nfe) {
      throw new ParseException("Port is not a number.");
    }

    if (port < 0 || port > 65535) {
      throw new ParseException("Invalid port value. (0 <= port <= 65535).");
    }

    return port;
  }

  /**
   * Opens a server that receives streams of file contents, which
   * is then summed and created into an uploaded GC logfile.
   * @param args command-line user input.
   */
  public static void main(String[] args) {
    final Options options = makeServerOptions();
    int port;
    try {
      port = parseOptions(options, args);
    } catch (ParseException pe) {
      logger.error("Parsing failed. Reason : " + pe.getMessage());
      return;
    }

    // start the server
    final LogUploadServer serverInstance = new LogUploadServer(port);
    try {
      serverInstance.start();
      serverInstance.blockUntilShutdown();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      logger.error("Server failed to initiate.", ioe);
      serverInstance.stop();
    } catch (InterruptedException ite) {
      ite.printStackTrace();
      logger.error("Thread interrupted.", ite);
      serverInstance.stop();
    }
  }
}
