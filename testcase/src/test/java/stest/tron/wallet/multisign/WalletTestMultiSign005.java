package stest.tron.wallet.multisign;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Proposal;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class WalletTestMultiSign005 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final byte[] witness001Address = PublicMethod.getFinalAddress(witnessKey);
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[1];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
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

  @Test(enabled = true, groups = {"multisig", "smoke"})
  public void testMultiSignForProposal() {
    long needcoin = updateAccountPermissionFee + multiSignFee * 3;
    Assert.assertTrue(PublicMethod.sendcoin(witness001Address, needcoin + 10000000L,
        foundationAddress, foundationKey, blockingStubFull));

    ecKey1 = new ECKey(Utils.getRandom());
    manager1Address = ecKey1.getAddress();
    manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    manager2Address = ecKey2.getAddress();
    manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceBefore = PublicMethod.queryAccount(witness001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = witnessKey;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":2}]},"
            + "\"witness_permission\":{\"type\":1,\"permission_name\":\"owner\",\"threshold\":1,\""
            + "keys\":[{\"address\":\"" + PublicMethod.getAddressString(witnessKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign.accountPermissionUpdate(
        accountPermissionJson, witness001Address, witnessKey,
        blockingStubFull, ownerKeyString);
  //Create a proposal
    Long proposalValue = 819699L;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, proposalValue);
    Assert.assertTrue(
        PublicMethodForMultiSign.createProposalWithPermissionId(witness001Address, witnessKey,
            proposalMap, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    Integer proposalId = 0;
    for (Proposal proposal : listProposals.get().getProposalsList()) {
      for (Map.Entry<Long, Long> entry : proposal.getParametersMap().entrySet()) {
        if (entry.getValue() == proposalValue) {
          proposalId = (int) proposal.getProposalId();
          break;
        }
      }
    }
    final Integer finalProposalId = proposalId;

    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethodForMultiSign.approveProposalWithPermission(
        witness001Address, witnessKey, proposalId,
        true, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Delete proposal list after approve
    Assert.assertTrue(PublicMethodForMultiSign.deleteProposalWithPermissionId(
        witness001Address, witnessKey, proposalId, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long balanceAfter = PublicMethod.queryAccount(witness001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertTrue(balanceBefore - balanceAfter >= needcoin);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


