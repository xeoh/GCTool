/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.client;

import io.grpc.ManagedChannel;

import edu.kaist.algo.analysis.GcAnalyzedData;
import edu.kaist.algo.service.AnalyzedResult;
import edu.kaist.algo.service.LogAnalysisGrpc;
import edu.kaist.algo.service.TicketInfo;

/**
 * Class to request analyzed data of log file.
 */
public class AnalysisDataRequester {
  private static final String UNRECOGNIZED_RESULT_MSG = "Unknown status received.";

  private final LogAnalysisGrpc.LogAnalysisBlockingStub blockingStub;

  public AnalysisDataRequester(ManagedChannel channel) {
    blockingStub = LogAnalysisGrpc.newBlockingStub(channel);
  }

  /**
   * Request analyzed data.
   *
   * @param ticketNumber ticket number to request
   * @return analyzed data
   */
  public GcAnalyzedData requestAnalysisData(long ticketNumber) {
    TicketInfo ticketInfo = TicketInfo
        .newBuilder()
        .setTicketNumber(ticketNumber)
        .build();
    AnalyzedResult result = blockingStub.requestAnalyzedData(ticketInfo);

    switch (result.getStatus()) {
      case NOT_READY:
      case ANALYZING:
        // fall through
      case ERROR:
        System.out.println(result.getMessage());
        return null;
      case COMPLETED:
        break;
      default:
        System.out.println(UNRECOGNIZED_RESULT_MSG);
        return null;
    }
    return result.getResultData();
  }
}
