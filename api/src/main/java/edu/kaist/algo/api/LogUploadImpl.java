/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.api;

import com.google.protobuf.ByteString;

import edu.kaist.algo.api.jobs.LogAnalyzeJob;
import edu.kaist.algo.service.AnalysisStatus;
import edu.kaist.algo.service.FileInfo;
import edu.kaist.algo.service.FileInfoResult;
import edu.kaist.algo.service.LogUploadGrpc;
import edu.kaist.algo.service.UploadRequest;
import edu.kaist.algo.service.UploadResult;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Defines a service that firstly receives the information about
 * the file to be uploaded, and then receives a stream of logfile
 * contents and writes on a file on the server.
 */
public class LogUploadImpl implements LogUploadGrpc.LogUpload {
  private static final Logger logger =
      LoggerFactory.getLogger(LogUploadImpl.class);
  private final Ticketer ticketer;
  private final Map<Long, FileOutputStream> ticketToFos = new HashMap<>();
  private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

  /**
   * Creates LogUploadImpl instance.
   * @param ticketer the ticketer instance to use for redis interactions
   */
  LogUploadImpl(Ticketer ticketer) {
    this.ticketer = ticketer;
  }

  /**
   * Single-shot gRPC service receiving file information from the client.
   *
   * <p>From the file name, a new file is created. Resulting file
   * is used to write contents received from the client in
   * logUpload() method.
   *
   * @param fileinfo The file information such as file name, date, etc.
   * @param responseObserver StreamObserver type from client.
   */
  @Override
  public void infoUpload(FileInfo fileinfo,
                         StreamObserver<FileInfoResult> responseObserver) {
    File uploadedFile = new File(FilenameUtils.getName(fileinfo.getFilename()));

    // file should not already exist
    if (uploadedFile.exists()) {
      responseObserver.onError(new FileExistsException());
    } else {
      long ticket = ticketer.issueTicket();
      ticketer.setLogFile(ticket, uploadedFile.getName());
      ticketer.setStatus(ticket, AnalysisStatus.NOT_READY);

      FileInfoResult result = FileInfoResult.newBuilder()
          .setSuccessful(true)
          .setId(ticket)
          .build();

      responseObserver.onNext(result);
      responseObserver.onCompleted();
    }
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

    return new StreamObserver<UploadRequest>() {
      private long ticketNum;
      int totalsize = 0;

      @Override
      public void onNext(UploadRequest uploadrequest) {
        ticketNum = uploadrequest.getId();

        FileOutputStream fos = ticketToFos.get(ticketNum);
        if (fos == null) {
          try {
            fos = FileUtils.openOutputStream(new File(ticketer.getLogFile(ticketNum)));
          } catch (IOException ioe) {
            logger.error("Could not open file.", ioe);
            responseObserver.onCompleted();
            return;
          }
          ticketToFos.put(ticketNum, fos);
        }

        ByteString bytestring = uploadrequest.getContents();
        int size = bytestring.size();
        try {
          bytestring.writeTo(fos);
        } catch (IOException ie) {
          logger.error("Error occurred during file receiving : " + ie.getMessage());
        }
        totalsize += size;
      }

      @Override
      public void onError(Throwable thrown) {
        Status status = Status.fromThrowable(thrown);
        logger.error("Log receiving failed : " + status.getDescription());
        ticketer.deleteResource(ticketNum);
        ticketToFos.remove(ticketNum);

        UploadResult result = UploadResult.newBuilder().setFilesize(totalsize)
            .setSuccessful(false).build();
        responseObserver.onNext(result);
      }

      @Override
      public void onCompleted() {
        ticketToFos.remove(ticketNum);

        final String logFile = ticketer.getLogFile(ticketNum);
        ticketer.setMeta(ticketNum, logFile, totalsize);
        backgroundExecutor.submit(new LogAnalyzeJob(ticketer, ticketNum));

        UploadResult result = UploadResult.newBuilder()
            .setFilesize(totalsize)
            .setSuccessful(true)
            .build();
        responseObserver.onNext(result);
        responseObserver.onCompleted();
      }
    };
  }
}
