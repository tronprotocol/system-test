package stest.tron.wallet.dailybuild.ratelimit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;

@Slf4j
public class RateLimite001 {

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelRealSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub realBlockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  //soliditynode is SR-2 RPC solidity port
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String realSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  private ManagedChannel channelFull2 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull2 = null;
  private String fullnode2 = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
          .get(1);





  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    channelRealSolidity = ManagedChannelBuilder.forTarget(realSoliditynode)
        .usePlaintext()
        .build();
    realBlockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelRealSolidity);
    channelFull2 = ManagedChannelBuilder.forTarget(fullnode2)
            .usePlaintext()
            .build();
    blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);


  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Rate limit IpQpsStrategy for ListWitness interface")
  public void test1QpsStrategyForListWitnessInterface() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 20) {
      blockingStubFull2.listWitnesses(EmptyMessage.newBuilder().build());
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 5000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Rate limit IpQpsStrategy for ListNodes interface")
  public void test2IpQpsStrategyForListNodesInterface() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    while (repeatTimes++ < 20) {
      blockingStubFull2.listNodes(EmptyMessage.newBuilder().build());
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 5000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Rate limit IpQpsStrategy for getBlockByNum2 "
      + "interface on fullnode's solidity service")
  public void test3IpQpsStrategyForgetBlockByNum2ResourceInterfaceOnFullnodeSolidityService() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(5);

    while (repeatTimes++ < 20) {
      blockingStubSolidity.getBlockByNum2(builder.build());
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 5000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Rate limit QpsStrategy for getBlockByNum "
      + "interface on fullnode's solidity service")
  public void test4QpsStrategyForgetBlockByNumResourceInterfaceOnFullnodeSolidityService() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(5);
    while (repeatTimes++ < 20) {
      blockingStubSolidity.getBlockByNum(builder.build());
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 5000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Rate limit IpQpsStrategy for getBlockByNum2 "
      + "interface on real solidity")
  public void test5IpQpsStrategyForgetBlockByNum2ResourceInterfaceOnFullnodeSolidityService() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(5);

    while (repeatTimes++ < 20) {
      realBlockingStubSolidity.getBlockByNum2(builder.build());
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 5000);
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Rate limit QpsStrategy for getBlockByNum "
      + "interface on real solidity")
  public void test6QpsStrategyForgetBlockByNumResourceInterfaceOnFullnodeSolidityService() {
    Long startTimeStamp = System.currentTimeMillis();
    Integer repeatTimes = 0;
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(5);
    while (repeatTimes++ < 20) {
      realBlockingStubSolidity.getBlockByNum(builder.build());
    }
    Long endTimesStap = System.currentTimeMillis();
    logger.info("startTimeStamp - endTimesStap:" + (endTimesStap - startTimeStamp));
    Assert.assertTrue(endTimesStap - startTimeStamp > 5000);
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}