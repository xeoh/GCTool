/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import edu.kaist.algo.model.GcEvent;

import org.apache.commons.lang3.StringUtils;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.ParsingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The <code>CmsLogParser</code> is responsible to parse the CMS GC log file.
 */
public class CmsLogParser {

  private static final Logger logger = LoggerFactory.getLogger(CmsLogParser.class);

  private static final Pattern WRITER_THREAD = Pattern.compile("<writer thread='(\\d+)'/>");
  private static final Pattern MULTI_LINE = Pattern.compile(".*GC.*\\[CMS$");

  @VisibleForTesting
  int currentThread;
  private final Map<Integer, String> threadToIncompleteLine = new HashMap<>();
  private final ParseRunner<Object> parseRunner;

  public CmsLogParser() {
    final CmsGcLogRule parser = Parboiled.createParser(CmsGcLogRule.class);
    parseRunner = new BasicParseRunner<>(parser.InputLine());
  }

  /**
   * Parses the given log file to the list of GcEvent.
   *
   * @param path path of the log file
   * @return list of GcEvent
   */
  public List<GcEvent> parse(final Path path) {
    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
      return parse(bufferedReader.lines());
    } catch (IOException ioe) {
      logger.error("Cannot open the file.", ioe);
    }
    return Collections.emptyList();
  }

  /**
   * Parses the given logs to the list of GcEvent.
   *
   * @param logs stream of gc log lines
   * @return list of GcEvent
   */
  public List<GcEvent> parse(final Stream<String> logs) {
    return logs.map(this::parseLine).filter(Objects::nonNull)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  @VisibleForTesting
  GcEvent parseLine(String line) {
    if (line.startsWith("<writer")) {
      currentThread = parseWriterThreadId(line);
    } else if (MULTI_LINE.matcher(line).matches()) {
      threadToIncompleteLine.put(currentThread, line);
    } else {
      if (threadToIncompleteLine.containsKey(currentThread)) {
        final String previousLine = threadToIncompleteLine.remove(currentThread);
        line = previousLine + line;
      }
      return parseGcEvent(line);
    }
    return null;
  }

  @VisibleForTesting
  int parseWriterThreadId(String line) {
    final Matcher m = WRITER_THREAD.matcher(line);
    if (m.matches()) {
      try {
        return Integer.parseInt(m.group(1));
      } catch (NumberFormatException nfe) {
        logger.error("writer thread id must be number : " + line, nfe);
      }
    }
    return -1;
  }

  @VisibleForTesting
  GcEvent parseGcEvent(final String line) {
    final ParsingResult<Object> result = parseRunner.run(line);
    if (!result.matched) {
      return null;
    }
    final GcEventNode node = (GcEventNode) result.resultValue;
    final GcEvent.LogType logType = convertLogType(node);
    final String typeDetails =
        Stream.concat(Stream.of(node), node.children().stream())
            .map(GcEventNode::typeAndDetail)
            .collect(Collectors.joining("; "));
    final GcEvent.Builder builder = GcEvent.newBuilder()
        .setThread(currentThread)
        .setTimestamp(MoreObjects.firstNonNull(node.timestamp(), 0L))
        .setLogType(logType)
        .setPauseTime(MoreObjects.firstNonNull(node.elapsedTime(), 0.0))
        .setUserTime(MoreObjects.firstNonNull(node.user(), 0.0))
        .setSysTime(MoreObjects.firstNonNull(node.sys(), 0.0))
        .setRealTime(MoreObjects.firstNonNull(node.real(), 0.0))
        .setCmsCpuTime(MoreObjects.firstNonNull(node.cmsCpuTime(), 0.0))
        .setCmsWallTime(MoreObjects.firstNonNull(node.cmsWallTime(), 0.0))
        .setTypeDetail(typeDetails);
    if (logType == GcEvent.LogType.CMS_FINAL_REMARK) {
      final Optional<GcEventNode> weakRefTimeOption = node.children().stream()
          .filter(e -> StringUtils.equals(e.type(), "weak refs processing"))
          .findFirst();
      if (weakRefTimeOption.isPresent()) {
        double refTime = MoreObjects.firstNonNull(weakRefTimeOption.get().elapsedTime(), 0.0);
        builder.setRefTime(refTime);
      }
    }
    return builder.build();
  }

  @VisibleForTesting
  GcEvent.LogType convertLogType(final GcEventNode node) {
    if (StringUtils.startsWith(node.type(), "CMS-concurrent-")) {
      return GcEvent.LogType.CMS_CONCURRENT;
    }
    if (StringUtils.equals(node.type(), "Full GC")) {
      return GcEvent.LogType.FULL_GC;
    }
    // GC Pause that starts with "GC" can be either one of the following:
    //   * CMS initial mark
    //   * CMS remark
    //   * ParNew (Minor GC)
    //   * ParNew that ended up as Full GC
    if (StringUtils.equals(node.type(), "GC")) {
      if (StringUtils.equals(node.detail(), "CMS Initial Mark")) {
        return GcEvent.LogType.CMS_INIT_MARK;
      }
      if (StringUtils.equals(node.detail(), "CMS Final Remark")) {
        return GcEvent.LogType.CMS_FINAL_REMARK;
      }
      final List<String> types = node.children().stream()
          .map(GcEventNode::type).collect(Collectors.toList());
      // If ParNew ended up in full GC, [CMS ... ] appears in the log.
      final boolean isFullGc = Collections.indexOfSubList(
          types, ImmutableList.of("ParNew", "CMS")) != -1;
      return isFullGc ? GcEvent.LogType.FULL_GC : GcEvent.LogType.MINOR_GC;
    }
    throw new IllegalArgumentException("Log type must be specified. Check the log.");
  }
}
