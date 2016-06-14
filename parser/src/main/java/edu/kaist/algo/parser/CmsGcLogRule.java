/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.parser;

import org.apache.commons.lang3.StringUtils;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.Label;
import org.parboiled.annotations.SuppressSubnodes;

/**
 * PEG (Parsing Expression Grammar) for the CMS GC log.
 *
 * <p>This PEG is for parsing the one line of CMS GC logs. The line should be complete. That is,
 * it should not be cut off by another thread's interference.
 *
 * <p>Beware: CMS-related logs are not supported yet.
 *
 * <p>Following options are required:
 * <ul>
 * <li>-XX:+UseConcMarkSweepGC</li>
 * <li>-XX:+UnlockDiagnosticVMOptions</li>
 * <li>-XX:+LogVMOutput</li>
 * <li>-XX:+PrintGCDetails</li>
 * <li>-XX:+PrintGCTimeStamps</li>
 * </ul>
 *
 * <p>PEG (whitespaces are ignored for conciseness):
 * <pre>
 * InputLine <- CMSConcurrentEvent / (Event UserSysRealTimes)
 * Event <- (Time ': ')? '[' TypeAndDetail (Event)* UsageAndElapsedTime ']'
 * Time <- Digits '.' Digits ' secs'?
 * Digits <- [0-9]+
 * TypeAndDetail <- Type ('(' Detail ')')? ': '?
 * Type <- 'GC' / 'ParNew' / 'CMS' / 'Full GC' / 'Metaspace' / '1 CMS-initial-mark'
 *       / 'YG occupancy' / 'Rescan (parallel)' / 'weak refs processing' / 'class unloading'
 *       / 'scrub symbol table' / 'scrub string table' / '1 CMS-remark'
 * Detail <- 'System.gc()' / !')'+
 * UsageAndElapsedTime <- UsageChange? (', ' Event)? (', ' Time)?
 * UsageChange <- (Size '-&<span>gt;</span>')? UsageWithTotal
 * UsageWithTotal <- Size '(' Size ')'
 * Size <- Digits 'K '
 * UserSysRealTimes <- '[ Times: user=' Time ' sys=' Time ', real=' Time ']'
 * CMSConcurrentEvent <- Time ': ' '[CMS-concurrent-' !':]'+
 *                      (': ' Time '/' Time '] ' UserSysRealTimes)?
 * </pre>
 */
@BuildParseTree
public class CmsGcLogRule extends BaseParser<Object> {

  Rule InputLine() {
    return Sequence(
        push(GcEventNode.builder()),
        FirstOf(CMSConcurrentEvent(), Sequence(Event(), UserSysRealTimes())),
        push(popAsNode().build())
    );
  }

  Rule Event() {
    return Sequence(
        Optional(
            TimeLong(), ": ",
            swap() && push(popAsNode().timestamp(popAsLong()))
        ),
        "[", TypeAndDetail(), " ",
        ZeroOrMore(
            push(GcEventNode.builder()),
            Event(),
            swap() && push(popAsNode().addChild(popAsNode().build()))
        ),
        " ", UsageAndElapsedTime(), "] "
    );
  }

  Rule CMSConcurrentEvent() {
    return Sequence(
        TimeLong(), ": ",
        swap() && push(popAsNode().timestamp(popAsLong())),
        "[CMS-concurrent-",
        OneOrMore(NoneOf(":]")),
        push(popAsNode().type("CMS-concurrent-" + match())),
        Optional(
            ": ",
            TimeDouble(),
            "/",
            TimeDouble(),
            swap3() && push(popAsNode().cmsCpuTime(popAsDouble())),
            push(popAsNode().cmsWallTime(popAsDouble())),
            "] ",
            UserSysRealTimes()
        )
    );
  }

  Rule TypeAndDetail() {
    return Sequence(
        Type(),
        push(popAsNode().type(match())),
        Optional(" ", "(", Detail(), push(popAsNode().detail(match())), ")"),
        Optional(": ")
    );
  }

  @SuppressSubnodes
  Rule Type() {
    return FirstOf("GC", "ParNew", "CMS", "Full GC", "Metaspace", "1 CMS-initial-mark",
        "YG occupancy", "Rescan (parallel)", "weak refs processing", "class unloading",
        "scrub symbol table", "scrub string table", "1 CMS-remark");
  }

  @SuppressSubnodes
  Rule Detail() {
    return FirstOf("System.gc()", OneOrMore(NoneOf(")")));
  }

  Rule UsageAndElapsedTime() {
    return Sequence(
        Optional(UsageChange()),
        Optional(", ",
            push(GcEventNode.builder()),
            Event(), // Metaspace
            swap() && push(popAsNode().addChild(popAsNode().build()))
        ),
        Optional(", ",
            TimeDouble(),
            swap() && push(popAsNode().elapsedTime(popAsDouble()))
        )
    );
  }

  Rule UsageChange() {
    return Sequence(
        Optional(
            Size(), "-&gt;",
            swap() && push(popAsNode().prevUsage(popAsLong()))
        ),
        UsageWithTotal()
    );
  }

  Rule UsageWithTotal() {
    return Sequence(
        Size(),
        "(", Size(), ")",
        swap3() && push(popAsNode().afterUsage(popAsLong())),
        push(popAsNode().capacity(popAsLong()))
    );
  }

  Rule Size() {
    return Sequence(
        Digits(),
        push(Long.valueOf(match())),
        WhiteSpace(), "K "
    );
  }

  Rule UserSysRealTimes() {
    return Sequence(
        "[", "Times: ", "user=", TimeDouble(), " sys=", TimeDouble(), ", real=", TimeDouble(), "]",
        swap4() && push(popAsNode().user(popAsDouble())),
        push(popAsNode().sys(popAsDouble())),
        push(popAsNode().real(popAsDouble()))
    );
  }

  @Label("Time")
  @SuppressSubnodes
  Rule TimeDouble() {
    return Sequence(
        Sequence(Digits(), ".", Digits()),
        push(Double.valueOf(match())),
        Optional(" secs")
    );
  }

  @Label("Time")
  @SuppressSubnodes
  Rule TimeLong() {
    return Sequence(
        Sequence(Digits(), ".", Digits()),
        push(Long.valueOf(StringUtils.remove(match(), ".")))
    );
  }

  @SuppressSubnodes
  Rule Digits() {
    return OneOrMore(Digit());
  }

  Rule Digit() {
    return CharRange('0', '9');
  }

  @SuppressSubnodes
  Rule WhiteSpace() {
    return ZeroOrMore(AnyOf(" \t\f"));
  }

  @Override
  protected Rule fromStringLiteral(String string) {
    return string.endsWith(" ")
        ? Sequence(String(string.substring(0, string.length() - 1)), WhiteSpace())
        : String(string);
  }

  protected Double popAsDouble() {
    return (Double) pop();
  }

  protected GcEventNode.Builder popAsNode() {
    return (GcEventNode.Builder) pop();
  }

  protected Long popAsLong() {
    return (Long) pop();
  }
}
