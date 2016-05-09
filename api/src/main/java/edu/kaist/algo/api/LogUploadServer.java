package edu.kaist.algo.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;

import edu.kaist.algo.client.GcServiceGrpc;
import edu.kaist.algo.client.UploadRequest;
import edu.kaist.algo.client.UploadResult;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * A logfile receiving server.
 */
public class LogUploadServer {
  private static final Logger logger = Logger.getLogger(LogUploadServer.class.getName());

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

    @Override
    public StreamObserver<UploadRequest> logUpload(
        final StreamObserver<UploadResult> responseObserver) {

      FileOutputStream outputstream;
      try {
        outputstream = FileUtils.openOutputStream(new File("uploaded.log"));
      } catch (FileNotFoundException fnfe) {
        System.err.println("File cannot be created or cannot be opened.");
        return null;
      } catch (IOException ioe) {
        System.err.println("Could not open file output stream.");
        return null;
      }

      return new StreamObserver<UploadRequest>() {
        int totalsize = 0;
        boolean successful = false;
        int id = 1; // temporary default number
        private UploadResult result;

        @Override
        public void onNext(UploadRequest uploadrequest) {
          ByteString bytestring = uploadrequest.getContents();
          int size = bytestring.size();
          try {
            bytestring.writeTo(outputstream);
          } catch (IOException ie) {
            // any way to abort the gRPC stream??
            System.err.println("Error occurred during file receiving.");
            successful = false;
          }
          totalsize += size;
        }

        @Override
        public void onError(Throwable thrown) {
          Status status = Status.fromThrowable(thrown);
          logger.log(Level.WARNING, "Log Receiving Failed: {0}", status);
        }

        @Override
        public void onCompleted() {
          // build Upload Result
          successful = true;
          result = UploadResult.newBuilder().setFilesize(totalsize)
              .setId(id).setSuccessful(successful).build();
          responseObserver.onNext(result);
          responseObserver.onCompleted();
        }
      };
    }
  }

  /**
   * Opens a server that receives streams of file contents, which
   * is then summed and created into an uploaded GC logfile.
   * @param args command-line user input.
   */
  public static void main(String[] args) {
    int port; // default value
    if (args.length == 0) {
      port = 50051; // default value
    } else if (args.length == 1) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException nfe) {
        System.err.println("Argument " + args[0] + "not appropriate.");
        return;
      }
    } else {
      System.err.println("Give only the port number in int format.");
      return;
    }

    // start the server
    final LogUploadServer serverInstance = new LogUploadServer(port);
    try {
      serverInstance.start();
      serverInstance.blockUntilShutdown();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      logger.warning("Server failed to initiate.");
    } catch (InterruptedException ite) {
      ite.printStackTrace();
      logger.warning("Thread interrupted.");
      serverInstance.stop();
    }
  }
}