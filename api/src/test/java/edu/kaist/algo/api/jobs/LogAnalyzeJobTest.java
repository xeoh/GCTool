package edu.kaist.algo.api.jobs;

import static org.junit.Assert.assertEquals;

import com.fiftyonred.mock_jedis.MockJedisPool;

import edu.kaist.algo.api.GcTestUtils;
import edu.kaist.algo.api.Ticketer;
import edu.kaist.algo.service.AnalysisStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import redis.clients.jedis.JedisPoolConfig;

@RunWith(JUnit4.class)
public class LogAnalyzeJobTest {

  @Test
  public void testAnalyzeJob() throws Exception {
    final MockJedisPool jedisPool = new MockJedisPool(new JedisPoolConfig(), "localhost");
    final Ticketer ticketer = new Ticketer(jedisPool);
    final long ticketNum = 1;
    ticketer.setLogFile(ticketNum, "src/test/resources/hotspot_long.log");

    new LogAnalyzeJob(ticketer, ticketNum).run();

    assertEquals(AnalysisStatus.COMPLETED, ticketer.getStatus(ticketNum));
    assertEquals(GcTestUtils.parseFromResource("hotspot_long.log"), ticketer.getResult(ticketNum));
  }
}
