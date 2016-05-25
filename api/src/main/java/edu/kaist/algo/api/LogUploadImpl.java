package edu.kaist.algo.api;

import com.google.protobuf.ByteString;

import edu.kaist.algo.client.FileInfo;
import edu.kaist.algo.client.FileUploadResult;
import edu.kaist.algo.client.LogUploadGrpc;
import edu.kaist.algo.client.UploadRequest;
import edu.kaist.algo.client.UploadResult;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Defines a service that receives a stream of logfile and creating a file on
 * the server.
 */
public class LogUploadImpl implements LogUploadGrpc.LogUpload {
  private static final Logger logger =
      LoggerFactory.getLogger(LogUploadImpl.class);
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
   * The file must not exit already.
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
