package stest.tron.wallet.common.client.utils;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.CommonParameter;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.Sha256Hash;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.protos.contract.ProposalContract.ProposalDeleteContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import stest.tron.wallet.common.client.Configuration;

/** Utility class for governance operations: proposals, voting, witnesses, and brokerage. */
@Slf4j
public class GovernanceHelper {

  /** Check whether the FreezeV2 proposal (UnfreezeDelayDays) is enabled on-chain. */
  public static Boolean freezeV2ProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays
        .getProposalName(), blockingStubFull) > 0;
  }


  /** Check whether the TronPower (AllowNewResourceModel) proposal is enabled on-chain. */
  public static Boolean tronPowerProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getChainParametersValue(ProposalEnum.GetAllowNewResourceModel
        .getProposalName(), blockingStubFull) == 1;
  }

  /** Check whether the AllowDynamicEnergy proposal is enabled on-chain. */
  public static Boolean getAllowDynamicEnergyProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getChainParametersValue(ProposalEnum.GetAllowDynamicEnergy
        .getProposalName(), blockingStubFull) == 1;
  }



  /** Get the current memo fee from the proposal parameters. */
  public static Long getProposalMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getChainParametersValue(ProposalEnum.GetMemoFee.getProposalName(),blockingStubFull);
  }

  /** Query the memo fee price history string from the full node. */
  public static String getMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getMemoFee(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Query the energy price history string from the full node. */
  public static String getEnergyPrice(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getEnergyPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Query the energy price history string from the solidity node. */
  public static String getEnergyPriceSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return blockingStubFull.getEnergyPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Query the bandwidth price history string from the full node. */
  public static String getBandwidthPrices(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getBandwidthPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Query the bandwidth price history string from the solidity node. */
  public static String getBandwidthPricesSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return blockingStubFull.getBandwidthPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Vote for one or more witness candidates using the given witness-to-voteCount map. */
  public static boolean voteWitness(
      byte[] ownerAddress,
      String priKey,
      HashMap<byte[], Long> witnessMap,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    VoteWitnessContract.Builder builder = VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    for (byte[] address : witnessMap.keySet()) {
      VoteWitnessContract.Vote.Builder voteBuilder = VoteWitnessContract.Vote.newBuilder();
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(witnessMap.get(address));
      builder.addVotes(voteBuilder.build());
    }

    VoteWitnessContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.voteWitnessAccount2(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** Create a new governance proposal with the given parameter key-value pairs. */
  public static boolean createProposal(
      byte[] ownerAddress,
      String priKey,
      HashMap<Long, Long> parametersMap,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalCreateContract.Builder builder = ProposalCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.putAllParameters(parametersMap);

    ProposalCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalCreate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** Approve or revoke approval for an existing governance proposal. */
  public static boolean approveProposal(
      byte[] ownerAddress,
      String priKey,
      long id,
      boolean isAddApproval,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalApproveContract.Builder builder = ProposalApproveContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);
    builder.setIsAddApproval(isAddApproval);
    ProposalApproveContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalApprove(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Delete (cancel) an existing governance proposal by its ID. */
  public static boolean deleteProposal(
      byte[] ownerAddress, String priKey, long id, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    ProposalDeleteContract.Builder builder = ProposalDeleteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);

    ProposalDeleteContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalDelete(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Look up a chain parameter value by its proposal name string. */
  public static Long getChainParametersValue(String proposalName,WalletGrpc.WalletBlockingStub blockingStubFull) {
    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      if(getChainParameters.get().getChainParameter(i).getKey().equals(proposalName)) {
        return getChainParameters.get().getChainParameter(i).getValue();
      }
    }

    return 0L;


  }

  /** List all active witnesses from the full node. */
  public static Optional<GrpcAPI.WitnessList> listWitnesses(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    GrpcAPI.WitnessList witnessList =
        blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(witnessList);
  }

  /** List all active witnesses from the solidity node. */
  public static Optional<GrpcAPI.WitnessList> listWitnessesFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    GrpcAPI.WitnessList witnessList =
        blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(witnessList);
  }

  /** Calculate expected witness allowances over a block range based on votes and brokerage. */
  public static Map<String, Long> getAllowance2(
      Long startNum, Long endNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    final String blackHole =
        Configuration.getByPath("testng.conf").getString("defaultParameter.blackHoleAddress");
    Long totalCount = 0L;
    Map<String, Integer> witnessBlockCount = new HashMap<>();
    Map<String, Long> witnessBrokerage = new HashMap<>();
    Map<String, Long> witnessVoteCount = new HashMap<>();
    Map<String, Long> witnessAllowance = new HashMap<>();
    List<Protocol.Witness> witnessList =
        PublicMethod.listWitnesses(blockingStubFull).get().getWitnessesList();
    for (Protocol.Witness witness : witnessList) {
      witnessVoteCount.put(
          ByteArray.toHexString(witness.getAddress().toByteArray()), witness.getVoteCount());
      GrpcAPI.BytesMessage bytesMessage =
          GrpcAPI.BytesMessage.newBuilder().setValue(witness.getAddress()).build();
      Long brokerager = blockingStubFull.getBrokerageInfo(bytesMessage).getNum();
      witnessBrokerage.put(ByteArray.toHexString(witness.getAddress().toByteArray()), brokerager);
      totalCount += witness.getVoteCount();
    }
    Optional<Protocol.TransactionInfo> infoById = null;
    for (Long k = startNum; k < endNum; k++) {
      String witnessAdd =
          ByteArray.toHexString(
              PublicMethod.getBlock(k, blockingStubFull)
                  .getBlockHeader()
                  .getRawData()
                  .getWitnessAddress()
                  .toByteArray());
      witnessBlockCount.put(witnessAdd, witnessBlockCount.getOrDefault(witnessAdd, 0) + 1);
      List<Transaction> transList =
          PublicMethod.getBlock(k, blockingStubFull).getTransactionsList();
      for (Transaction tem : transList) {
        String txid =
            ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    tem.getRawData().toByteArray()));
        logger.info("----ss txid:" + txid);
        infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
        Long packingFee = infoById.get().getPackingFee();

        witnessAllowance.put(
            witnessAdd, witnessAllowance.getOrDefault(witnessAdd, 0L) + packingFee);
      }
    }

    logger.info("========totalCount:" + totalCount);
    List<Protocol.ChainParameters.ChainParameter> chainParaList =
        blockingStubFull
            .getChainParameters(EmptyMessage.newBuilder().build())
            .getChainParameterList();
    Long witness127PayPerBlock = 0L;
    Long witnessPayPerBlock = 0L;
    for (Protocol.ChainParameters.ChainParameter para : chainParaList) {
      if ("getWitness127PayPerBlock".equals(para.getKey())) {
        witness127PayPerBlock = para.getValue();
      }
      if ("getWitnessPayPerBlock".equals(para.getKey())) {
        witnessPayPerBlock = para.getValue();
      }
    }
    logger.info(
        "witness127PayPerBlock:"
            + witness127PayPerBlock
            + "\n witnessPayPerBlock:"
            + witnessPayPerBlock);

    for (Map.Entry<String, Long> entry : witnessBrokerage.entrySet()) {
      logger.info("-----witnessBrokerage   " + entry.getKey() + " : " + entry.getValue());
    }
    for (Map.Entry<String, Long> entry : witnessVoteCount.entrySet()) {
      logger.info("-----witnessVoteCount   " + entry.getKey() + " : " + entry.getValue());
    }
    for (Map.Entry<String, Integer> entry : witnessBlockCount.entrySet()) {
      logger.info("-----witnessBlockCount   " + entry.getKey() + " : " + entry.getValue());
    }

    for (Map.Entry<String, Long> entry : witnessVoteCount.entrySet()) {
      String witnessAdd = entry.getKey();
      logger.info(
          "----witnessAdd:"
              + witnessAdd
              + " block count:"
              + witnessBlockCount.get(witnessAdd)
              + "    all: "
              + witnessAllowance.getOrDefault(witnessAdd, 0L));
      Long pay =
          (witnessBlockCount.get(witnessAdd) * witnessPayPerBlock
                  + (endNum - startNum) * witness127PayPerBlock * entry.getValue() / totalCount
                  + witnessAllowance.getOrDefault(witnessAdd, 0L))
              * witnessBrokerage.get(witnessAdd)
              / 100;

      witnessAllowance.put(witnessAdd, pay);
      logger.info("******  " + witnessAdd + " : " + pay);
    }
    return witnessAllowance;
  }

  /** Query the brokerage ratio (0-100) for a given witness address. */
  public static long getBrokerage(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    GrpcAPI.BytesMessage bytesMessage =
        GrpcAPI.BytesMessage.newBuilder().setValue(addressBs).build();
    Long brokerager = blockingStubFull.getBrokerageInfo(bytesMessage).getNum();
    return brokerager;
  }

  /** Update the brokerage ratio for a witness (SR) account. */
  public boolean updateBrokerage(
      byte[] owner, int brokerage, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(ByteString.copyFrom(owner)).setBrokerage(brokerage);
    TransactionExtention transactionExtention =
        blockingStubFull.updateBrokerage(updateBrokerageContract.build());
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }
}
