package stest.tron.wallet.onlinestress;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.ChainParameters;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class TestApproveProposal extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  //Witness 47.93.33.201
  //Witness 123.56.10.6
  //Wtiness 39.107.80.135
  //Witness 47.93.184.2
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  private final byte[] witness002Address = PublicMethod.getFinalAddress(witnessKey2);
  private final byte[] witness003Address = PublicMethod.getFinalAddress(witnessKey3);
  private final byte[] witness004Address = PublicMethod.getFinalAddress(witnessKey4);
  private final byte[] witness005Address = PublicMethod.getFinalAddress(witnessKey5);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true, groups = {"stress"})
  public void testApproveProposal() {
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
  //proposalMap.put(25L, 1L);
    proposalMap.put(27L, 0L);
  //proposalMap.put(28L, 1L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
  final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));
  //Get proposal list after approve
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
  //logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));
  //just test keys
    String[] witnessKey = {

        "369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77",
        "9FD8E129DE181EA44C6129F727A6871440169568ADE002943EAD0E7A16D8EDAC",

    };
  byte[] witnessAddress;
    for (String key : witnessKey) {
      witnessAddress = PublicMethod.getFinalAddress(key);
      PublicMethod.approveProposal(witnessAddress, key, proposalId,
          true, blockingStubFull);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Test(enabled = true, groups = {"stress"})
  public void testGetChainParameters() {
    //Set the default map
    HashMap<String, Long> defaultCommitteeMap = new HashMap<String, Long>();
    defaultCommitteeMap.put("MAINTENANCE_TIME_INTERVAL", 300000L);
    defaultCommitteeMap.put("ACCOUNT_UPGRADE_COST", 9999000000L);
    defaultCommitteeMap.put("CREATE_ACCOUNT_FEE", 100000L);
    defaultCommitteeMap.put("TRANSACTION_FEE", 10L);
    defaultCommitteeMap.put("ASSET_ISSUE_FEE", 1024000000L);
    defaultCommitteeMap.put("WITNESS_PAY_PER_BLOCK", 32000000L);
    defaultCommitteeMap.put("WITNESS_STANDBY_ALLOWANCE", 115200000000L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT", 0L);
    defaultCommitteeMap.put("CREATE_NEW_ACCOUNT_BANDWIDTH_RATE", 1L);

    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info("Index is:" + i);
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
    }

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}

