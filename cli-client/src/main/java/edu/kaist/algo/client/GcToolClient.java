package edu.kaist.algo.client;

import com.google.common.annotations.VisibleForTesting;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import edu.kaist.algo.analysis.GcAnalyzedData;

public class GcToolClient {
  private final ManagedChannel channel;

  /**
   * Construct client to server at {@code host:port}.
   *
   * @param host The host name or IP address to bind to.
   * @param port The port number of the host.
   */
  public GcToolClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext(true)
        .build();
  }

  /**
   * shutdown client server.
   *
   * @throws InterruptedException case of Client ended during shutdown
   */
  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * The ParsedOptions class contains the parsed command-line options.
   */
  @VisibleForTesting
  static class ParsedOptions {
    public enum ClientAction {
      NONE,
      UPLOAD_FILE,
      REQUEST_ANALYZED_DATA
    }

    private ClientAction action;
    private int port;
    private String filename;
    private String host;
    private long requestTicket;
    private boolean beautifyResult;

    public ClientAction getAction() {
      return this.action;
    }

    public int getPort() {
      return port;
    }

    public String getFilename() {
      return filename;
    }

    public String getHost() {
      return host;
    }

    public long getRequestTicket() {
      return requestTicket;
    }

    public boolean getBeautifyResult() {
      return beautifyResult;
    }

    private ParsedOptions(ParsedOptionBuilder builder) {
      this.action = builder.action;
      this.port = builder.port;
      this.filename = builder.filename;
      this.host = builder.host;
      this.requestTicket = builder.requestTicket;
      this.beautifyResult = builder.beautifyResult;
    }

    public static class ParsedOptionBuilder {
      ClientAction action = ClientAction.NONE;
      private int port;
      private String filename;
      private String host;
      private long requestTicket;
      private boolean beautifyResult;

      public void setPort(int port) {
        this.port = port;
      }

      public void setFilename(String filename) {
        this.action = ClientAction.UPLOAD_FILE;
        this.filename = filename;
      }

      public void setHost(String host) {
        this.host = host;
      }

      public void setRequestTicket(long ticket) {
        this.action = ClientAction.REQUEST_ANALYZED_DATA;
        this.requestTicket = ticket;
      }

      public void setBeautifyResult(boolean beautify) {
        this.beautifyResult = beautify;
      }

      public ParsedOptions build() {
        return new ParsedOptions(this);
      }
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
        .required(false)
        .build();
    Option port = Option.builder("p")
        .hasArg(true)
        .argName("port")
        .desc("give the port number")
        .required(true)
        .build();
    Option filename = Option.builder("f")
        .hasArg(true)
        .argName("filename")
        .desc("give the absolute / relative path of the file to upload")
        .required(false)
        .build();
    Option requestData = Option.builder("rd")
        .hasArg(true)
        .argName("ticket_to_request")
        .desc("give ticket number to get analyzed data")
        .required(false)
        .build();
    Option beautifyData = Option.builder()
        .longOpt("beautify")
        .hasArg(false)
        .desc("beautify analyzed data")
        .required(false)
        .build();
    options.addOption(host);
    options.addOption(port);
    options.addOption(filename);
    options.addOption(requestData);
    options.addOption(beautifyData);

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
      ParsedOptions.ParsedOptionBuilder optionBuilder = new ParsedOptions.ParsedOptionBuilder();

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

      String host = cmd.getOptionValue("h", "localhost");
      if (host.isEmpty()) {
        System.err.println("Must give a proper host name!");
        return null;
      }

      if (cmd.hasOption("f")) {
        String filename = cmd.getOptionValue("f");
        optionBuilder.setFilename(filename);

        if (filename.isEmpty()) {
          System.err.println("Must give a proper existing file name.");
          return null;
        }
      } else if (cmd.hasOption("rd")) {
        try {
          long ticket = Long.parseLong(cmd.getOptionValue("rd"));
          optionBuilder.setRequestTicket(ticket);
          optionBuilder.setBeautifyResult(cmd.hasOption("beautify"));
        } catch (NumberFormatException nfe) {
          System.err.println("Must give a number to option 'ticket_to_request'");
        }
      }

      optionBuilder.setPort(port);
      optionBuilder.setHost(host);

      return optionBuilder.build();
    } catch (ParseException pe) {
      System.err.println("Parsing failed. Reason : " + pe.getMessage());
    }
    return null;
  }

  // printing help
  private static void help(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Main", options);
  }

  private void uploadLog(ParsedOptions parsedOptions) {
    LogUploader logUploader = new LogUploader(channel);

    // look for the file to upload
    File logfile = LogUploader.lookForFile(parsedOptions.getFilename());
    if (logfile == null) {
      System.out.println("File does not exist!");
      return;
    }

    // give the filename to server to open on server
    long ticket = logUploader.uploadInfo(parsedOptions.getFilename());

    // Send and upload the file
    try {
      FileInputStream inputStream = FileUtils.openInputStream(logfile);
      logUploader.uploadLog(ticket, inputStream);
    } catch (InterruptedException ie) {
      System.out.println("File upload failed due to interruption.");
    } catch (IOException ioe) {
      System.out.println("Could not open input stream.");
    }
  }

  private void requestAnalyzedData(ParsedOptions parsedOptions) {
    AnalysisDataRequester requester = new AnalysisDataRequester(channel);
    GcAnalyzedData result = requester.requestAnalysisData(parsedOptions.getRequestTicket());

    if(parsedOptions.getBeautifyResult()) {
      System.out.println(LogUtil.beautifyAnalyzedData(result));
    } else {
      System.out.println(result);
    }
  }

  /**
   * Main method of cli-client.
   *
   * @param args argument of clients
   */
  public static void main(String[] args) {
    // since grpc uses j.u.l logger, redirect j.u.l to slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    // parse command-line options
    final Options options = makeOptions();
    final ParsedOptions parsedOptions = parseOptions(options, args);
    if (parsedOptions == null) {
      System.out.println("Client cannot start due to parsing failure.");
      help(options);
      return;
    }

    GcToolClient client = new GcToolClient(parsedOptions.getHost(), parsedOptions.getPort());
    System.out.println("***Client now bound to server.");

    switch (parsedOptions.getAction()) {
      case UPLOAD_FILE:
        client.uploadLog(parsedOptions);
        break;
      case REQUEST_ANALYZED_DATA:
        client.requestAnalyzedData(parsedOptions);
        break;
      case NONE:
        help(options);
        break;
      default:
        help(options);
        break;
    }

    // shutdown the client
    try {
      client.shutdown();
    } catch (InterruptedException ie) {
      System.out.println("Client ended during shutdown");
      System.exit(-1);
    }
  }
}
