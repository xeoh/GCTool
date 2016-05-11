package edu.kaist.algo.parser;

import java.util.regex.Pattern;

/**
 * Patterns for CMS log format.
 */
public class CmsLogPatterns {
  private CmsLogPatterns() {
    // Do not instantiate.
  }

  static final Pattern WRITER_THREAD = Pattern.compile("<writer thread='(\\d+)'/>");
  static final Pattern MULTI_LINE = Pattern.compile(".*GC.*\\[CMS$");
  static final Pattern USER_SYS_REAL_EXTRACT = Pattern.compile("user=(.+) sys=(.+), real=(.+)");
  static final Pattern FULL_GC_START = Pattern.compile("^(\\d+.\\d{3}): \\[Full GC(?: \\(([^\\)]+)\\))?");
  static final Pattern CMS_FULL_GC = Pattern.compile( "\\[CMS(?: \\(([^\\)]+)\\))?: (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\] (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\),[^,]+, (\\d+.\\d+) secs\\]");
  static final Pattern METASPACE = Pattern.compile("\\[Metaspace: (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\)\\]");
  static final Pattern USER_SYS_REAL_TIME = Pattern.compile("\\[Times: (.+) secs\\]");
  static final Pattern GC = Pattern.compile("^(\\d+.\\d{3}): \\[GC \\(([^\\)]+)\\).*\\[Times: (.+) secs\\].*$");
  static final Pattern PAR_NEW = Pattern.compile( "\\[ParNew: (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\] (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\]");
  static final Pattern PAR_NEW_TRIGGERD_FULL_GC = Pattern.compile( "\\[ParNew(?: \\(([^\\)]+)\\))?: (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\]\\d+.\\d+: \\[CMS(?: \\(([^\\)]+)\\))?: (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\] (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\), \\[Metaspace: (\\d+)K-&gt;(\\d+)K\\((\\d+)K\\)\\], (\\d+.\\d+) secs\\]");
  static final Pattern CMS_INITIAL_MARK = Pattern.compile( "\\[1 CMS-initial-mark: (\\d+)K\\((\\d+)K\\)\\] (\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\]");
  static final Pattern CMS_REMARK = Pattern.compile( "\\[1 CMS-remark: (\\d+)K\\((\\d+)K\\)\\] (\\d+)K\\((\\d+)K\\), (\\d+.\\d+) secs\\]");
  static final Pattern YG_OCCUPANCY = Pattern.compile("\\[YG occupancy: (\\d+) K \\((\\d+) K\\)\\]");
  static final Pattern WEAK_REFS_PROCESSING = Pattern.compile("\\[weak refs processing, (\\d+.\\d+) secs\\]");
  static final Pattern CLASS_UNLOADING = Pattern.compile("\\[class unloading, (\\d+.\\d+) secs\\]");
  static final Pattern SCRUB_SYMBOL_TABLE = Pattern.compile("\\[scrub symbol table, (\\d+.\\d+) secs\\]");
  static final Pattern SCRUB_STRING_TABLE = Pattern.compile("\\[scrub string table, (\\d+.\\d+) secs\\]");
}
