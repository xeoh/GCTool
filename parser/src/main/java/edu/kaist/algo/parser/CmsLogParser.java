package edu.kaist.algo.parser;

import edu.kaist.algo.model.GcEvent;

import org.apache.commons.lang3.StringUtils;
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
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The <code>CmsLogParser</code> is responsible to parse the CMS GC log file.
 *
 * @author ducky
 */
public class CmsLogParser {

  private static final Logger logger = LoggerFactory.getLogger(CmsLogParser.class);

  private int currentThread;
  private Map<Integer, String> threadToIncompleteLine = new HashMap<>();

  /**
   * Parses the given log file to the list of GcEvent.
   * @param path path of the log file
   * @return list of GcEvent
   */
  public List<GcEvent> parse(Path path) {
    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
      return parse(bufferedReader.lines());
    } catch (IOException ioe) {
      logger.error("Cannot open the file.", ioe);
    }
    return Collections.emptyList();
  }

  /**
   * Parses the given logs to the list of GcEvent.
   * @param logs stream of gc log lines
   * @return list of GcEvent
   */
  public List<GcEvent> parse(Stream<String> logs) {
    return logs.map(this::parseLine).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private GcEvent parseLine(String line) {
    if (line.startsWith("<writer")) {
      final Matcher m = CmsLogPatterns.WRITER_THREAD.matcher(line);
      if (m.matches()) {
        currentThread = Integer.parseInt(m.group(1));
      }
    } else {
      if (CmsLogPatterns.MULTI_LINE.matcher(line).matches()) {
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

  private UserSysRealTime parseUserSysRealTime(final String line) {
    final Matcher m = CmsLogPatterns.USER_SYS_REAL_EXTRACT.matcher(line);
    if (m.find()) {
      final double user = Double.parseDouble(m.group(1));
      final double sys = Double.parseDouble(m.group(2));
      final double real = Double.parseDouble(m.group(3));
      return UserSysRealTime.of(user, sys, real);
    } else {
      throw new IllegalArgumentException("Cannot parse : " + line);
    }
  }

  private GcEvent parseGcEvent(String line) {
    if (line.matches("^\\d+.\\d{3}.*")) {
      if (line.contains("Full GC")) {
        Matcher matcher = CmsLogPatterns.FULL_GC_START.matcher(line);
        if (matcher.find()) {
          final long timestamp = Long.parseLong(StringUtils.remove(matcher.group(1), "."));
          final String cause = matcher.group(2);
          matcher = CmsLogPatterns.CMS_FULL_GC.matcher(line);
          if (matcher.find()) {
            String cmsDetail = matcher.group(1);
            int prevOldGenUsage = Integer.parseInt(matcher.group(2));
            int afterOldGenUsage = Integer.parseInt(matcher.group(3));
            int oldGenCapacity = Integer.parseInt(matcher.group(4));
            String oldGcTime = matcher.group(5);
            int prevHeapUsage = Integer.parseInt(matcher.group(6));
            int afterHeapUsage = Integer.parseInt(matcher.group(7));
            int heapCapacity = Integer.parseInt(matcher.group(8));
            String gcTime = matcher.group(9);
            matcher = CmsLogPatterns.USER_SYS_REAL_TIME.matcher(line);
            if (matcher.find()) {
              UserSysRealTime times = parseUserSysRealTime(matcher.group(1));
              return GcEvent.newBuilder()
                  .setThread(currentThread)
                  .setTimestamp(timestamp)
                  .setLogType(GcEvent.LogType.FULL_GC)
                  .setPauseTime(Double.parseDouble(gcTime))
                  .setUserTime(times.user)
                  .setSysTime(times.sys)
                  .setRealTime(times.real)
                  .build();
            }
          }
        }
      } else {
        final Matcher gcMatcher = CmsLogPatterns.GC.matcher(line);
        if (gcMatcher.matches()) {
          final long timestamp = Long.parseLong(StringUtils.remove(gcMatcher.group(1), "."));
          final String cause = gcMatcher.group(2);
          final String gcTimeString = gcMatcher.group(3);
          final UserSysRealTime times = parseUserSysRealTime(gcTimeString);

          if (StringUtils.equals(cause, "CMS Initial Mark")) {
            Matcher matcher = CmsLogPatterns.CMS_INITIAL_MARK.matcher(line);
            if (matcher.find()) {
              int oldGenUsage = Integer.parseInt(matcher.group(1));
              int oldGenCapacity = Integer.parseInt(matcher.group(2));
              int heapUsage = Integer.parseInt(matcher.group(3));
              int heapCapacity = Integer.parseInt(matcher.group(4));
              String time = matcher.group(5);
              return GcEvent.newBuilder()
                  .setThread(currentThread)
                  .setTimestamp(timestamp)
                  .setLogType(GcEvent.LogType.CMS_INIT_MARK)
                  .setPauseTime(Double.parseDouble(time))
                  .setUserTime(times.user)
                  .setSysTime(times.sys)
                  .setRealTime(times.real)
                  .build();
            }
          } else if (StringUtils.equals(cause, "CMS Final Remark")) {
            Matcher matcher = CmsLogPatterns.CMS_REMARK.matcher(line);
            if (matcher.find()) {
              int oldGenUsage = Integer.parseInt(matcher.group(1));
              int oldGenCapacity = Integer.parseInt(matcher.group(2));
              int heapUsage = Integer.parseInt(matcher.group(3));
              int heapCapacity = Integer.parseInt(matcher.group(4));
              String time = matcher.group(5);

              matcher = CmsLogPatterns.YG_OCCUPANCY.matcher(line);
              if (matcher.find()) {
                int youngGenUsage = Integer.parseInt(matcher.group(1));
                int youngGenCapacity = Integer.parseInt(matcher.group(2));
              }
              matcher = CmsLogPatterns.WEAK_REFS_PROCESSING.matcher(line);
              if (matcher.find()) {
                String weakRefsTime = matcher.group(1);
                return GcEvent.newBuilder()
                    .setThread(currentThread)
                    .setTimestamp(timestamp)
                    .setLogType(GcEvent.LogType.CMS_FINAL_REMARK)
                    .setPauseTime(Double.parseDouble(time))
                    .setUserTime(times.user)
                    .setSysTime(times.sys)
                    .setRealTime(times.real)
                    .setRefTime(Double.parseDouble(weakRefsTime))
                    .build();
              }
            }
          } else {
            if (line.matches(".*\\[ParNew[^\\[]+\\[CMS.*")) {
              // Full GC
              Matcher parNewFullGcMatcher = CmsLogPatterns.PAR_NEW_TRIGGERD_FULL_GC.matcher(line);
              if (parNewFullGcMatcher.find()) {
                String parNewDetail = parNewFullGcMatcher.group(1);
                int prevYoungSize = Integer.parseInt(parNewFullGcMatcher.group(2));
                int afterYoungSize = Integer.parseInt(parNewFullGcMatcher.group(3));
                int youngCapacity = Integer.parseInt(parNewFullGcMatcher.group(4));
                String youngGcTime = parNewFullGcMatcher.group(5);
                String cmsDetail = parNewFullGcMatcher.group(6);
                int prevOldSize = Integer.parseInt(parNewFullGcMatcher.group(7));
                int afterOldSize = Integer.parseInt(parNewFullGcMatcher.group(8));
                int oldCapacity = Integer.parseInt(parNewFullGcMatcher.group(9));
                String oldGcTime = parNewFullGcMatcher.group(10);
                int prevHeapSize = Integer.parseInt(parNewFullGcMatcher.group(11));
                int afterHeapSize = Integer.parseInt(parNewFullGcMatcher.group(12));
                int heapCapacity = Integer.parseInt(parNewFullGcMatcher.group(13));
                int prevMetaspaceSize = Integer.parseInt(parNewFullGcMatcher.group(14));
                int afterMetaspaceSize = Integer.parseInt(parNewFullGcMatcher.group(15));
                int metaspaceCapacity = Integer.parseInt(parNewFullGcMatcher.group(16));
                String totalGcTime = parNewFullGcMatcher.group(17);
                return GcEvent.newBuilder()
                    .setThread(currentThread)
                    .setTimestamp(timestamp)
                    .setLogType(GcEvent.LogType.FULL_GC)
                    .setPauseTime(Double.parseDouble(totalGcTime))
                    .setUserTime(times.user)
                    .setSysTime(times.sys)
                    .setRealTime(times.real)
                    .build();
              }
            } else {
              // Young GC
              Matcher parNewMatcher = CmsLogPatterns.PAR_NEW.matcher(line);
              if (parNewMatcher.find()) {
                int prevYoungSize = Integer.parseInt(parNewMatcher.group(1));
                int afterYoungSize = Integer.parseInt(parNewMatcher.group(2));
                int youngCapacity = Integer.parseInt(parNewMatcher.group(3));
                String youngGcTime = parNewMatcher.group(4);
                int prevHeapSize = Integer.parseInt(parNewMatcher.group(5));
                int afterHeapSize = Integer.parseInt(parNewMatcher.group(6));
                int heapCapacity = Integer.parseInt(parNewMatcher.group(7));
                String totalGcTime = parNewMatcher.group(8);
                return GcEvent.newBuilder()
                    .setThread(currentThread)
                    .setTimestamp(timestamp)
                    .setLogType(GcEvent.LogType.MINOR_GC)
                    .setPauseTime(Double.parseDouble(totalGcTime))
                    .setUserTime(times.user)
                    .setSysTime(times.sys)
                    .setRealTime(times.real)
                    .build();
              }
            }
          }
        }
      }
    }
    return null;
  }

  private static class UserSysRealTime {
    public final double user;
    public final double sys;
    public final double real;

    private UserSysRealTime(double user, double sys, double real) {
      this.user = user;
      this.sys = sys;
      this.real = real;
    }

    public static UserSysRealTime of(double user, double sys, double real) {
      return new UserSysRealTime(user, sys, real);
    }
  }
}
