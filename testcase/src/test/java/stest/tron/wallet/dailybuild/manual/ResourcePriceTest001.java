package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class ResourcePriceTest001 {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidity = null;

  private String fullNodeSolidityPort =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(1);
  private ManagedChannel channelSolidityPort = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFullSolidityPort = null;

  private String pbftnode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list")
          .get(2);
  private ManagedChannel channelPbft = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubFullSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSolidityPort = ManagedChannelBuilder.forTarget(fullNodeSolidityPort)
        .usePlaintext()
        .build();
    blockingStubFullSolidityPort = WalletSolidityGrpc.newBlockingStub(channelSolidityPort);

    channelPbft = ManagedChannelBuilder.forTarget(pbftnode)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
  }


  @Test(enabled = true, description = "get memoFee grpc")
  public void test01GetMemoFee() {
    //get memoFee from chainParameters
    Long memoFeeChainParameters = PublicMethed.getProposalMemoFee(blockingStubFull);

    String memoFee = PublicMethed.getMemoFee(blockingStubFull);
    Assert.assertNotNull(memoFee);
    logger.info("memoFee is " + memoFee);
    long memoNow = Long.parseLong(memoFee.split(":")[2]);
    Assert.assertEquals(memoNow, memoFeeChainParameters.longValue());
  }

  @Test(enabled = true, description = "get energyPrice grpc")
  public void test02GetEnergyPrices() {
    //get energyPrice from chainParameters
    Long energyPriceChainParameters = PublicMethed.getChainParametersValue(
        ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull
    );

    //get energyPrice from grpc interface
    String energyPrice = PublicMethed.getEnergyPrice(blockingStubFull);
    Assert.assertNotNull(energyPrice);
    logger.info(energyPrice);
    logger.info("energyPrice is " + energyPrice);
    long energyPricesNow = Long.parseLong(energyPrice.split(":")[2]);
    Assert.assertEquals(energyPricesNow, energyPriceChainParameters.longValue());

    //request solidity and pbft interface and fullNode solidity port
    String energyPricesSolidity = PublicMethed.getEnergyPriceSolidity(blockingStubFullSolidity);
    String energyPricesSolidityPort = PublicMethed.getBandwidthPricesSolidity(blockingStubFullSolidityPort);
    String energyPricesPbft = PublicMethed.getEnergyPriceSolidity(blockingStubPbft);
    Assert.assertEquals(energyPrice, energyPricesSolidity);
    Assert.assertEquals(energyPrice, energyPricesPbft);
    Assert.assertEquals(energyPrice, energyPricesSolidityPort);
  }

  @Test(enabled = true, description = "get bandwidthPrices grpc")
  public void test02GetBandwidthPrices() {
    //get BandwidthPrices from chainParameters
    Long bandwidthPriceChainParameters = PublicMethed.getChainParametersValue(
        ProposalEnum.getTransactionFee.getProposalName(), blockingStubFull
    );

    //get BandwidthPrices from grpc interface
    String bandwidthPrices = PublicMethed.getBandwidthPrices(blockingStubFull);
    Assert.assertNotNull(bandwidthPrices);
    logger.info(bandwidthPrices);
    logger.info("bandwidthPrices is " + bandwidthPrices);
    long bandwidthPricesNow = Long.parseLong(bandwidthPrices.split(":")[2]);
    Assert.assertEquals(bandwidthPricesNow, bandwidthPriceChainParameters.longValue());

    //request solidity and pbft and fullNode solidity port
    String bandwidthPricesSolidity = PublicMethed.getBandwidthPricesSolidity(blockingStubFullSolidity);
    String bandwidthPricesSolidityPort = PublicMethed.getBandwidthPricesSolidity(blockingStubFullSolidityPort);
    String bandwidthPricesPbft = PublicMethed.getBandwidthPricesSolidity(blockingStubPbft);
    Assert.assertEquals(bandwidthPrices, bandwidthPricesSolidity);
    Assert.assertEquals(bandwidthPrices, bandwidthPricesPbft);
    Assert.assertEquals(bandwidthPrices, bandwidthPricesSolidityPort);
  }

}
