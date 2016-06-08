/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.api;

import io.grpc.stub.StreamObserver;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.service.AnalysisStatus;
import edu.kaist.algo.service.AnalyzedResult;
import edu.kaist.algo.service.LogAnalysisGrpc;
import edu.kaist.algo.service.TicketInfo;

import redis.clients.jedis.JedisPool;

/**
 * LogAnalysis service implementation.
 *
 * <p>categories of protocol
 * <ul>
 *   <li>RequestAnalyzedData</li>
 * </ul>
 */
public class LogAnalysisImpl implements LogAnalysisGrpc.LogAnalysis {
  private static final String NOT_READY_MSG = "The file to be analyzed is not ready";
  private static final String COMPLETED_MSG = "Log is analysed successfully";
  private static final String ANALYZING_MSG = "Server is analyzing log. Please wait.";
  private static final String ERROR_MSG = "Error occurred during analysis.";

  private static final Logger logger = LoggerFactory.getLogger(LogAnalysisImpl.class);

  private final Ticketer ticketer;

  /**
   * Consturctor of LogAnalysisImpl.
   *
   * @param ticketer ticketer instance
   */
  public LogAnalysisImpl(Ticketer ticketer) {
    this.ticketer = ticketer;
  }

  @Override
  public void requestAnalyzedData(TicketInfo request,
                                  StreamObserver<AnalyzedResult> responseObserver) {
    AnalysisStatus status = ticketer.getStatus(request.getTicketNumber());
    AnalyzedResult.Builder result = AnalyzedResult.newBuilder()
        .setStatus(status);

    switch (status) {
      case NOT_READY:
        result.setMessage(NOT_READY_MSG);
        responseObserver.onNext(result.build());
        break;
      case ANALYZING:
        result.setMessage(ANALYZING_MSG);
        responseObserver.onNext(result.build());
        break;
      case ERROR:
        result.setMessage(ERROR_MSG);
        responseObserver.onNext(result.build());
        break;
      case COMPLETED:
        result.setMessage(COMPLETED_MSG);

        final GcAnalyzedData data = ticketer.getResult(request.getTicketNumber());
        if (data != null) {
          result.setResultData(data);
          responseObserver.onNext(result.build());
        } else {
          result.setStatus(AnalysisStatus.ERROR).setMessage(ERROR_MSG);
          responseObserver.onNext(result.build());
        }
        break;
      default:
        break;
    }

    responseObserver.onCompleted();
  }
}
