package stest.tron.wallet.committee;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Proposal;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray; import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestCommittee004 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  //Witness 47.93.33.201
  //Witness 123.56.10.6
  //Wtiness 39.107.80.135
  //Witness 47.93.184.2
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  //private final byte[] witness003Address = PublicMethod.getFinalAddress(witnessKey3);
  //private final byte[] witness004Address = PublicMethod.getFinalAddress(witnessKey4);
  //private final byte[] witness005Address = PublicMethod.getFinalAddress(witnessKey5);
  private final byte[] witness002Address = PublicMethod.getFinalAddress(witnessKey2);
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

  @Test(enabled = true, groups = {"smoke"})
  public void test1DeleteProposal() {
    PublicMethod.sendcoin(witness001Address, 1000000L,
        toAddress, testKey003, blockingStubFull);
    PublicMethod.sendcoin(witness002Address, 1000000L,
        toAddress, testKey003, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Create a proposal and approval it
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(1L, 99999L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
  final Integer proposalId = listProposals.get().getProposalsCount();
    Assert.assertTrue(PublicMethod.approveProposal(witness001Address, witnessKey,
        proposalId, true, blockingStubFull));
    logger.info(Integer.toString(listProposals.get().getProposals(0).getStateValue()));
  //The state is "pending", state value == 0
    Assert.assertTrue(listProposals.get().getProposals(0).getStateValue() == 0);
  //When the proposal isn't created by you, you can't delete it.
    Assert.assertFalse(PublicMethod.deleteProposal(witness002Address, witnessKey2,
        proposalId, blockingStubFull));
  //Cancel the proposal
    Assert.assertTrue(PublicMethod.deleteProposal(witness001Address, witnessKey,
        proposalId, blockingStubFull));
  //When the state is cancel, you can't delete it again.
    Assert.assertFalse(PublicMethod.deleteProposal(witness001Address, witnessKey,
        proposalId, blockingStubFull));
  //You can't delete an invalid proposal
    Assert.assertFalse(PublicMethod.deleteProposal(witness001Address, witnessKey,
        proposalId + 100, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals = Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getStateValue()));
  //The state is "cancel", state value == 3
    Assert.assertTrue(listProposals.get().getProposals(0).getStateValue() == 3);
  //When the state is cancel, you can't approval proposal
    Assert.assertFalse(PublicMethod.approveProposal(witness001Address, witnessKey,
        proposalId, true, blockingStubFull));
    Assert.assertFalse(PublicMethod.approveProposal(witness001Address, witnessKey,
        proposalId, false, blockingStubFull));
  }

  @Test(enabled = true, groups = {"smoke"})
  public void test2GetProposal() {
    //Create a proposal and approval it
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(1L, 999999999L);
    Assert.assertTrue(PublicMethod.createProposal(witness001Address, witnessKey,
        proposalMap, blockingStubFull));
  //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
  final Integer proposalId = listProposals.get().getProposalsCount();

    BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
        ByteArray.fromLong(Long.parseLong(proposalId.toString()))))
        .build();
    Proposal proposal = blockingStubFull.getProposalById(request);
    Optional<Proposal> getProposal = Optional.ofNullable(proposal);

    Assert.assertTrue(getProposal.isPresent());
    Assert.assertTrue(getProposal.get().getStateValue() == 0);
  //Invalid get proposal
    final Integer wrongProposalId = proposalId + 99;
    request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
        ByteArray.fromLong(Long.parseLong(wrongProposalId.toString()))))
        .build();
    proposal = blockingStubFull.getProposalById(request);
    getProposal = Optional.ofNullable(proposal);
    logger.info(Long.toString(getProposal.get().getCreateTime()));
    Assert.assertTrue(getProposal.get().getCreateTime() == 0);
  }

  @Test(enabled = false, groups = {"smoke"})
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
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
    }
    Assert.assertTrue(getChainParameters.get().getChainParameterCount() >= 10);
    Assert.assertTrue(getChainParameters.get()
        .getChainParameter(1).getValue() == 9999000000L);
    Assert.assertTrue(getChainParameters.get().getChainParameter(4)
        .getValue() == 1024000000L);
    Assert.assertTrue(getChainParameters.get().getChainParameter(7).getValue() == 0);
    Assert.assertTrue(getChainParameters.get().getChainParameter(8).getValue() == 1);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}

