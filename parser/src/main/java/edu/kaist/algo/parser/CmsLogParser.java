/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.parser;

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
import java.util.function.Consumer;
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

  private int currentThread;
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

  private GcEvent parseLine(String line) {
    if (line.startsWith("<writer")) {
      final Matcher m = WRITER_THREAD.matcher(line);
      if (m.matches()) {
        currentThread = Integer.parseInt(m.group(1));
      }
    } else {
      if (MULTI_LINE.matcher(line).matches()) {
        threadToIncompleteLine.put(currentThread, line);
        return null;
      }
      if (threadToIncompleteLine.containsKey(currentThread)) {
        final String previousLine = threadToIncompleteLine.remove(currentThread);
        line = previousLine + line;
      }
      return parseGcEvent(line);
    }
    return null;
  }

  private GcEvent parseGcEvent(final String line) {
    final ParsingResult<Object> result = parseRunner.run(line);
    if (result.matched) {
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
        final Optional<GcEventNode> weakRefsProcessingEvent = node.children().stream()
            .filter(e -> StringUtils.equals(e.type(), "weak refs processing"))
            .findFirst();
        if (weakRefsProcessingEvent.isPresent()) {
          builder.setRefTime(MoreObjects.firstNonNull(weakRefsProcessingEvent.get().elapsedTime(), 0.0));
        }
      }
      return builder.build();
    }
    return null;
  }

  private GcEvent.LogType convertLogType(final GcEventNode node) {
    if (StringUtils.equals(node.type(), "Full GC")) {
      return GcEvent.LogType.FULL_GC;
    } else if (StringUtils.equals(node.type(), "GC")) {
      if (StringUtils.equals(node.detail(), "CMS Initial Mark")) {
        return GcEvent.LogType.CMS_INIT_MARK;
      } else if (StringUtils.equals(node.detail(), "CMS Final Remark")) {
        return GcEvent.LogType.CMS_FINAL_REMARK;
      } else {
        final List<String> types = node.children().stream()
            .map(GcEventNode::type).collect(Collectors.toList());
        final boolean isFullGc = Collections.indexOfSubList(
            types, ImmutableList.of("ParNew", "CMS")) != -1;
        if (isFullGc) {
          return GcEvent.LogType.FULL_GC;
        } else {
          return GcEvent.LogType.MINOR_GC;
        }
      }
    } else if (StringUtils.startsWith(node.type(), "CMS-concurrent-")) {
      return GcEvent.LogType.CMS_CONCURRENT;
    }
    throw new IllegalArgumentException("Log type must be specified. Check the log.");
  }
}
