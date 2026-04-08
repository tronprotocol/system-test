package stest.tron.wallet.updateCompatibility;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.ShieldAddressInfo;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class MultisignOperatorErgodicTest extends TronBaseTest {

  final String updateName = Long.toString(System.currentTimeMillis());  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  Optional<ShieldAddressInfo> shieldAddressInfo;
  String shieldAddress;
  List<Note> shieldOutList = new ArrayList<>();
  Account firstAccount;
  ByteString assetAccountId1;
  ByteString assetAccountId2;
  Optional<ExchangeList> listExchange;
  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] multisignAccountAddress = ecKey3.getAddress();
  String multisignAccountKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] newAddress = ecKey4.getAddress();
  String newKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());  private String foundationZenTokenKey = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenOwnerKey");
  byte[] foundationZenTokenAddress = PublicMethod.getFinalAddress(foundationZenTokenKey);
  private String zenTokenId = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.zenTokenId");
  private byte[] tokenId = zenTokenId.getBytes();
  private Long zenTokenFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.zenTokenFee");
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private Long costTokenAmount = 8 * zenTokenFee;
  private Long sendTokenAmount = 3 * zenTokenFee;

  /**
   * constructor.
   */
  @BeforeSuite(enabled = false)
  public void beforeSuite() {
    if (PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getCreateTime() == 0) {
      PublicMethod.sendcoin(foundationZenTokenAddress, 20480000000000L, fromAddress,
          foundationKey2, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      String name = "shieldToken";
      Long start = System.currentTimeMillis() + 20000;
      Long end = System.currentTimeMillis() + 10000000000L;
      Long totalSupply = 15000000000000001L;
      String description = "This asset issue is use for exchange transaction stress";
      String url = "This asset issue is use for exchange transaction stress";
      PublicMethod.createAssetIssue(foundationZenTokenAddress, name, totalSupply, 1, 1,
          start, end, 1, description, url, 1000L, 1000L,
          1L, 1L, foundationZenTokenKey, blockingStubFull);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      Account getAssetIdFromThisAccount =
          PublicMethod.queryAccount(foundationZenTokenAddress, blockingStubFull);
      ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
      logger.info("AssetId:" + assetAccountId.toString());
    }
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    Assert.assertTrue(PublicMethod.sendcoin(multisignAccountAddress, 1000_000_000_000L, fromAddress,
        foundationKey2, blockingStubFull));
    //updatepermission权限，账户交易所需钱等前置条件写在这
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = manager1Key;
    ownerKeyString[1] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, multisignAccountAddress, multisignAccountKey,
            blockingStubFull, new String[]{multisignAccountKey}));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test001MultiSignGodicAccountTypeTransaction() {
    Assert.assertTrue(
        PublicMethodForMultiSign.setAccountId1(("" + System.currentTimeMillis()).getBytes(),
            multisignAccountAddress, multisignAccountKey, 2, blockingStubFull,
            permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.createAccountWhtiPermissionId(
        multisignAccountAddress, newAddress, multisignAccountKey, blockingStubFull,
        2, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.sendcoinWithPermissionId(
        newAddress, 100L, multisignAccountAddress, 2,
        multisignAccountKey, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.freezeBalanceWithPermissionId(
        multisignAccountAddress, 1000000L, 0, 2,
        multisignAccountKey, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.freezeBalanceGetEnergyWithPermissionId(
        multisignAccountAddress, 1000000L, 0, 1,
        multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.freezeBalanceForReceiverWithPermissionId(
        multisignAccountAddress, 1000000L, 0, 0,
        ByteString.copyFrom(newAddress),
        multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.unFreezeBalanceWithPermissionId(
        multisignAccountAddress, multisignAccountKey, 0, null,
        2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.unFreezeBalanceWithPermissionId(
        multisignAccountAddress, multisignAccountKey, 0, newAddress,
        2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(PublicMethodForMultiSign.updateAccountWithPermissionId(
        multisignAccountAddress, updateName.getBytes(), multisignAccountKey, blockingStubFull,
        2, permissionKeyString));
  }

  @Test(enabled = true)
  public void test002MultiSignGodicContractTypeTransaction() {
    Long maxFeeLimit = 1000000000L;
    //String contractName = "StorageAndCpu" + Integer.toString(randNum);
    String filePath = "./src/test/resources/soliditycode/walletTestMultiSign004.sol";
    String contractName = "timeoutTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethodForMultiSign.deployContractWithPermissionId(
        contractName, abi, code, "", maxFeeLimit,
        0L, 100, maxFeeLimit, "0", 0L, null,
        multisignAccountKey, multisignAccountAddress, blockingStubFull, permissionKeyString, 2);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString() != null);
    String txid;
    String initParmes = "\"" + "930" + "\"";
    txid = PublicMethodForMultiSign.triggerContractWithPermissionId(contractAddress,
        "testUseCpu(uint256)", initParmes, false,
        0, maxFeeLimit, "0", 0L, multisignAccountAddress,
        multisignAccountKey, blockingStubFull, permissionKeyString, 2);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethodForMultiSign.updateSettingWithPermissionId(
            contractAddress, 50, multisignAccountKey,
            multisignAccountAddress, 2, blockingStubFull, permissionKeyString));
    Assert.assertTrue(
        PublicMethodForMultiSign.updateEnergyLimitWithPermissionId(
            contractAddress, 50, multisignAccountKey,
            multisignAccountAddress, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethodForMultiSign
        .clearContractAbi(contractAddress, multisignAccountAddress, multisignAccountKey,
            blockingStubFull, 2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true)
  public void test003MultiSignGodicTokenTypeTransaction() {

    long now = System.currentTimeMillis();
    String name = "MultiSign001_" + Long.toString(now);
    long totalSupply = now;
    Long start = System.currentTimeMillis() + 5000;
    Long end = System.currentTimeMillis() + 1000000000;
    logger.info("try create asset issue");

    Assert.assertTrue(PublicMethodForMultiSign
        .createAssetIssueWithpermissionId(multisignAccountAddress, name, totalSupply, 1,
            1, start, end, 1, description, url, 2000L, 2000L,
            1L, 1L, multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    //Assert.assertTrue(PublicMethodForMultiSign.unFreezeAsset(multisignAccountAddress,
    //    multisignAccountKey,2,ownerKeyString,blockingStubFull));

    Account getAssetIdFromOwnerAccount;
    getAssetIdFromOwnerAccount = PublicMethod.queryAccount(
        multisignAccountAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromOwnerAccount.getAssetIssuedID();

    Assert.assertTrue(PublicMethodForMultiSign.transferAssetWithpermissionId(manager1Address,
        assetAccountId1.toByteArray(), 10, multisignAccountAddress,
        multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethodForMultiSign
        .updateAssetWithPermissionId(multisignAccountAddress, description.getBytes(), url.getBytes(),
            100L, 100L, multisignAccountKey,
            2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true)
  public void test004MultiSignGodicExchangeTypeTransaction() {

    ECKey ecKey22 = new ECKey(Utils.getRandom());
    byte[] secondExchange001Address = ecKey22.getAddress();
    String secondExchange001Key = ByteArray.toHexString(ecKey22.getPrivKeyBytes());
    Long secondTransferAssetToFirstAccountNum = 100000000L;

    long now = System.currentTimeMillis();
    String name2 = "exchange001_2_" + Long.toString(now);
    String name1 = "exchange001_1_" + Long.toString(now);
    final long totalSupply = 1000000001L;

    org.junit.Assert
        .assertTrue(PublicMethod.sendcoin(secondExchange001Address, 10240000000L, fromAddress,
            foundationKey2, blockingStubFull));
    org.junit.Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(fromAddress, 100000000000L, 0, 0,
            ByteString.copyFrom(secondExchange001Address),
            foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 5000L;
    Long end = System.currentTimeMillis() + 5000000L;
    org.junit.Assert
        .assertTrue(PublicMethod.createAssetIssue(secondExchange001Address, name2, totalSupply, 1,
            1, start, end, 1, description, url, 10000L, 10000L,
            1L, 1L, secondExchange001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    listExchange = PublicMethod.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(multisignAccountAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethod
        .queryAccount(secondExchange001Address, blockingStubFull);
    assetAccountId2 = getAssetIdFromThisAccount.getAssetIssuedID();

    firstAccount = PublicMethod.queryAccount(multisignAccountAddress, blockingStubFull);
    org.junit.Assert.assertTrue(PublicMethod.transferAsset(
        multisignAccountAddress, assetAccountId2.toByteArray(),
        secondTransferAssetToFirstAccountNum, secondExchange001Address,
        secondExchange001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    org.junit.Assert.assertTrue(
        PublicMethodForMultiSign.exchangeCreate1(
            assetAccountId1.toByteArray(), firstTokenInitialBalance,
            assetAccountId2.toByteArray(), secondTokenInitialBalance, multisignAccountAddress,
            multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    listExchange = PublicMethod.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    org.junit.Assert.assertTrue(
        PublicMethodForMultiSign.injectExchange1(
            exchangeId, assetAccountId1.toByteArray(), 100,
            multisignAccountAddress, multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    org.junit.Assert.assertTrue(
        PublicMethodForMultiSign.exchangeWithdraw1(
            exchangeId, assetAccountId1.toByteArray(), 200,
            multisignAccountAddress, multisignAccountKey, blockingStubFull, 2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethod.queryAccount(multisignAccountAddress, blockingStubFull);

    org.junit.Assert.assertTrue(
        PublicMethodForMultiSign
            .exchangeTransaction1(exchangeId, assetAccountId1.toByteArray(), 50, 1,
                multisignAccountAddress, multisignAccountKey, blockingStubFull,
                2, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethod.queryAccount(multisignAccountAddress, blockingStubFull);

    Assert.assertTrue(PublicMethodForMultiSign.participateAssetIssueWithPermissionId(
        secondExchange001Address,
        assetAccountId2.toByteArray(), 1, multisignAccountAddress, multisignAccountKey, 2,
        blockingStubFull, ownerKeyString));

  }

  @Test(enabled = true)
  public void test005MultiSignGodicShieldTransaction() {

    Assert.assertTrue(PublicMethod.transferAsset(multisignAccountAddress, tokenId,
        costTokenAmount, foundationZenTokenAddress, foundationZenTokenKey, blockingStubFull));
    //Args.setFullNodeAllowShieldedTransaction(true);
    shieldAddressInfo = PublicMethod.generateShieldAddress();
    shieldAddress = shieldAddressInfo.get().getAddress();
    logger.info("shieldAddress:" + shieldAddress);
    final Long beforeAssetBalance = PublicMethod.getAssetIssueValue(multisignAccountAddress,
        PublicMethod.queryAccount(foundationZenTokenKey, blockingStubFull).getAssetIssuedID(),
        blockingStubFull);
    final Long beforeBalance = PublicMethod
        .queryAccount(multisignAccountAddress, blockingStubFull).getBalance();
    final Long beforeNetUsed = PublicMethod
        .getAccountResource(multisignAccountAddress, blockingStubFull).getFreeNetUsed();

    String memo = "aaaaaaa";

    shieldOutList = PublicMethod.addShieldOutputList(shieldOutList, shieldAddress,
        "" + (sendTokenAmount - zenTokenFee), memo);

    Assert.assertTrue(PublicMethodForMultiSign.sendShieldCoin(
        multisignAccountAddress, sendTokenAmount,
        null, null,
        shieldOutList,
        null, 0,
        multisignAccountKey, blockingStubFull, 2, permissionKeyString));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true)
  public void test006MultiSignGodicWitnessTransaction() {
    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    ownerKeyString[0] = manager1Key;
    ownerKeyString[1] = manager2Key;
    PublicMethod.printAddress(newKey);
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethod.sendcoin(newAddress, 1000000_000_000L, fromAddress,
        foundationKey2, blockingStubFull));
    logger.info(accountPermissionJson);
    Assert.assertTrue(PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, newAddress, newKey,
            blockingStubFull, new String[]{newKey}));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long now = System.currentTimeMillis();
    String url = "MultiSign001_" + Long.toString(now) + ".com";
    Assert.assertTrue(PublicMethodForMultiSign.createWitness(url, newAddress,
        newKey, 2, permissionKeyString, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert
        .assertTrue(PublicMethodForMultiSign.updateWitness2(newAddress, "newWitness.com".getBytes(),
            newKey, 2, permissionKeyString, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String voteStr = Base58.encode58Check(newAddress);
    HashMap<String, String> smallVoteMap = new HashMap<String, String>();
    smallVoteMap.put(voteStr, "1");
    Assert.assertTrue(PublicMethodForMultiSign.voteWitnessWithPermissionId(
        smallVoteMap, multisignAccountAddress, multisignAccountKey, blockingStubFull,
        2, permissionKeyString));

  }

  @Test(enabled = true)
  public void test007MultiSignGodicProposalTypeTransaction() {

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(0L, 81000L);
    Assert.assertTrue(
        PublicMethodForMultiSign.createProposalWithPermissionId(newAddress, newKey,
            proposalMap, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethodForMultiSign.approveProposalWithPermission(
        newAddress, newKey, proposalId,
        true, 2, blockingStubFull, permissionKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    //Delete proposal list after approve
    Assert.assertTrue(PublicMethodForMultiSign.deleteProposalWithPermissionId(
        newAddress, newKey, proposalId, 2, blockingStubFull, permissionKeyString));
  }

  @Test(enabled = true)
  public void test008MultiSignGodicWithdrawBanlanceTransaction() {
    long MaintenanceTimeInterval = -1L;
    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      logger.info("Index is:" + i);
      logger.info(getChainParameters.get().getChainParameter(i).getKey());
      logger.info(Long.toString(getChainParameters.get().getChainParameter(i).getValue()));
      if (getChainParameters.get().getChainParameter(i).getKey()
          .equals("getMaintenanceTimeInterval")) {
        MaintenanceTimeInterval = getChainParameters.get().getChainParameter(i).getValue();
        break;
      }
    }

    try {
      Thread.sleep(MaintenanceTimeInterval);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(PublicMethodForMultiSign.withdrawBalance(newAddress, newKey,
        2, permissionKeyString, blockingStubFull));
  }
}

