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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A log file uploader for the client.
 */
public class LogUploader {
  private static final Logger logger = LoggerFactory.getLogger(LogUploader.class);

  private final LogUploadGrpc.LogUploadStub asyncStub;
  private final LogUploadGrpc.LogUploadBlockingStub blockingStub;
  private static final int BUFFER_SIZE = 1024;

  /**
   * Constructor of LogUploader.
   *
   * @param channel client's channel
   */
  public LogUploader(ManagedChannel channel) {
    asyncStub = LogUploadGrpc.newStub(channel);
    blockingStub = LogUploadGrpc.newBlockingStub(channel);
  }

  /**
   * Creates a responseObserver that can be used in the server,
   * and uses an uploadObserver to stream file contents to the server until it is empty.
   * When the upload is done, the server sends the results as UploadResult class,
   * which responseObserver uses to print out the upload summary.
   * @param inputStream File input stream to read in contents of file.
   * @throws InterruptedException Thrown when upload stream is interrupted.
   */
  public void uploadLog(FileInputStream inputStream) throws InterruptedException {
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
        bytestring = readFrom(inputStream, BUFFER_SIZE);
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
  public void uploadInfo(String filename) {
    FileInfo fileinfo = FileInfo.newBuilder().setFilename(filename).build();

    try {
      FileUploadResult result = blockingStub.infoUpload(fileinfo);
      System.out.println("File to write on opened on server : " + result.getSuccessful());
    } catch (StatusRuntimeException event) {
      logger.error("infoUpload failed : ", event.getStatus());
    }
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
}
