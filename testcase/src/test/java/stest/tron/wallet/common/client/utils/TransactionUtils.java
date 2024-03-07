package stest.tron.wallet.common.client.utils;

/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AccountContract.SetAccountIdContract;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawBalanceContract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;
import org.tron.protos.contract.MarketContract.MarketCancelOrderContract;
import org.tron.protos.contract.MarketContract.MarketSellAssetContract;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.protos.contract.ProposalContract.ProposalDeleteContract;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import org.tron.protos.contract.VoteAssetContractOuterClass;
import org.tron.protos.contract.VoteAssetContractOuterClass.VoteAssetContract;
import org.tron.protos.contract.WitnessContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;
import stest.tron.wallet.common.client.utils.ECKey.ECDSASignature;
@Slf4j
public class TransactionUtils {

  public static final int NORMALTRANSACTION = 0;
  public static final int UNEXECUTEDDEFERREDTRANSACTION = 1;
  public static final int EXECUTINGDEFERREDTRANSACTION = 2;
//  private static final Logger logger = LoggerFactory.getLogger("Transaction");
  private static final int RESERVE_BALANCE = 10;
  public static HashMap<ContractType,Class<?>> transactionMap = new HashMap<>();

  /**
   * constructor.
   */

  public static byte[] getHash(Transaction transaction) {
    Transaction.Builder tmp = transaction.toBuilder();
    //tmp.clearId();
    return Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), tmp.build().toByteArray());
  }

  /**
   * constructor.
   */

  public static byte[] getOwner(Transaction.Contract contract) {
    ByteString owner;
    try {
      switch (contract.getType()) {
        case AccountCreateContract:
          owner = contract.getParameter()
              .unpack(AccountCreateContract.class).getOwnerAddress();
          break;
        case TransferContract:
          owner = contract.getParameter().unpack(BalanceContract.TransferContract.class)
              .getOwnerAddress();
          break;
        case TransferAssetContract:
          owner = contract.getParameter()
              .unpack(AssetIssueContractOuterClass.TransferAssetContract.class).getOwnerAddress();
          break;
        case VoteAssetContract:
          owner = contract.getParameter()
              .unpack(VoteAssetContractOuterClass.VoteAssetContract.class)
              .getOwnerAddress();
          break;
        case VoteWitnessContract:
          owner = contract.getParameter().unpack(WitnessContract.VoteWitnessContract.class)
              .getOwnerAddress();
          break;
        case WitnessCreateContract:
          owner = contract.getParameter()
              .unpack(WitnessContract.WitnessCreateContract.class).getOwnerAddress();
          break;
        case AssetIssueContract:
          owner = contract.getParameter()
              .unpack(AssetIssueContractOuterClass.AssetIssueContract.class)
              .getOwnerAddress();
          break;
        case ParticipateAssetIssueContract:
          owner = contract.getParameter()
              .unpack(AssetIssueContractOuterClass.ParticipateAssetIssueContract.class)
              .getOwnerAddress();
          break;
        case CreateSmartContract:
          owner = contract.getParameter().unpack(SmartContractOuterClass.CreateSmartContract.class)
              .getOwnerAddress();
          break;
        default:
          return null;
      }
      return owner.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /**
   * constructor.
   */

  public static String getBase64FromByteString(ByteString sign) {
    byte[] r = sign.substring(0, 32).toByteArray();
    byte[] s = sign.substring(32, 64).toByteArray();
    byte v = sign.byteAt(64);
    if (v < 27) {
      v += 27; //revId -> v
    }
    ECDSASignature signature = ECDSASignature.fromComponents(r, s, v);
    return signature.toBase64();
  }

  /*
   * 1. check hash
   * 2. check double spent
   * 3. check sign
   * 4. check balance
   */

  /**
   * constructor.
   */

  public static boolean validTransaction(Transaction signedTransaction) {
    assert (signedTransaction.getSignatureCount()
        == signedTransaction.getRawData().getContractCount());
    List<Transaction.Contract> listContract = signedTransaction.getRawData().getContractList();
    byte[] hash = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), signedTransaction.getRawData().toByteArray());
    int count = signedTransaction.getSignatureCount();
    if (count == 0) {
      return false;
    }
    for (int i = 0; i < count; ++i) {
      try {
        Transaction.Contract contract = listContract.get(i);
        byte[] owner = getOwner(contract);
        byte[] address = ECKey
            .signatureToAddress(hash, getBase64FromByteString(signedTransaction.getSignature(i)));
        if (!Arrays.equals(owner, address)) {
          return false;
        }
      } catch (SignatureException e) {
        e.printStackTrace();
        return false;
      }
    }
    return true;
  }

  /**
   * constructor.
   */

  public static Transaction sign(Transaction transaction, ECKey myKey) {
    ByteString lockSript = ByteString.copyFrom(myKey.getAddress());
    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();

    byte[] hash = Sha256Hash.hash(CommonParameter
        .getInstance().isECKeyCryptoEngine(), transaction.getRawData().toByteArray());
    List<Contract> listContract = transaction.getRawData().getContractList();
    for (int i = 0; i < listContract.size(); i++) {
      ECDSASignature signature = myKey.sign(hash);
      ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
      transactionBuilderSigned.addSignature(
          bsSign);//Each contract may be signed with a different private key in the future.
    }

    transaction = transactionBuilderSigned.build();
    try {
      String txId = ByteArray.toHexString(
              Sha256Hash.hash(
                      CommonParameter.getInstance().isECKeyCryptoEngine(),
                      transaction.getRawData().toByteArray()));
      String txType = transaction.getRawData().getContract(0).getType().name();
      logger.info("Transaction type: {}, id: {}" ,txType ,txId);
    }catch (Exception e){
      logger.warn("err transaction, please check, " + transaction + " err: " + e.getMessage());
    }
    return transaction;
  }


  public static byte[] generateContractAddress(Transaction trx, byte[] ownerAddress) {
    // get tx hash
    byte[] txRawDataHash = Sha256Hash
        .of(CommonParameter.getInstance().isECKeyCryptoEngine(), trx.getRawData().toByteArray())
        .getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }


  public static void initTransactionMap() {
    if(transactionMap.isEmpty()) {
      transactionMap.put(ContractType.TransferContract,TransferContract.class);
      transactionMap.put(ContractType.AccountUpdateContract, AccountUpdateContract.class);
      transactionMap.put(ContractType.VoteWitnessContract, VoteWitnessContract.class);
      transactionMap.put(ContractType.WitnessUpdateContract, WitnessUpdateContract.class);
      transactionMap.put(ContractType.AccountCreateContract,AccountCreateContract.class);
      transactionMap.put(ContractType.AccountPermissionUpdateContract,
          AccountPermissionUpdateContract.class);
      transactionMap.put(ContractType.AssetIssueContract, AssetIssueContract.class);
      transactionMap.put(ContractType.ClearABIContract, ClearABIContract.class);
      transactionMap.put(ContractType.CreateSmartContract,CreateSmartContract.class);
      transactionMap.put(ContractType.DelegateResourceContract, DelegateResourceContract.class);
      transactionMap.put(ContractType.ExchangeCreateContract, ExchangeCreateContract.class);
      transactionMap.put(ContractType.ExchangeInjectContract, ExchangeInjectContract.class);
      transactionMap.put(ContractType.ExchangeWithdrawContract, ExchangeWithdrawContract.class);
      transactionMap.put(ContractType.ExchangeTransactionContract,
          ExchangeTransactionContract.class);
      transactionMap.put(ContractType.FreezeBalanceV2Contract, FreezeBalanceV2Contract.class);
      transactionMap.put(ContractType.UnfreezeBalanceV2Contract, UnfreezeBalanceV2Contract.class);
      transactionMap.put(ContractType.UnDelegateResourceContract, UnDelegateResourceContract.class);
      transactionMap.put(ContractType.TriggerSmartContract, TriggerSmartContract.class);
      transactionMap.put(ContractType.WitnessCreateContract, WitnessCreateContract.class);
      transactionMap.put(ContractType.FreezeBalanceContract, FreezeBalanceContract.class);
      transactionMap.put(ContractType.MarketCancelOrderContract, MarketCancelOrderContract.class);
      transactionMap.put(ContractType.MarketSellAssetContract, MarketSellAssetContract.class);
      transactionMap.put(ContractType.ParticipateAssetIssueContract, ParticipateAssetIssueContract.class);
      transactionMap.put(ContractType.ShieldedTransferContract, ShieldedTransferContract.class);
      transactionMap.put(ContractType.SetAccountIdContract, SetAccountIdContract.class);
      transactionMap.put(ContractType.TransferAssetContract, TransferAssetContract.class);
      transactionMap.put(ContractType.UnfreezeAssetContract, UnfreezeAssetContract.class);
      transactionMap.put(ContractType.UnfreezeBalanceContract, UnfreezeBalanceContract.class);
      transactionMap.put(ContractType.UpdateAssetContract, UpdateAssetContract.class);
      transactionMap.put(ContractType.UpdateBrokerageContract, UpdateBrokerageContract.class);
      transactionMap.put(ContractType.UpdateEnergyLimitContract, UpdateEnergyLimitContract.class);
      transactionMap.put(ContractType.UpdateSettingContract, UpdateSettingContract.class);
      transactionMap.put(ContractType.WithdrawBalanceContract, WithdrawBalanceContract.class);
      transactionMap.put(ContractType.WithdrawExpireUnfreezeContract, WithdrawExpireUnfreezeContract.class);
      transactionMap.put(ContractType.ProposalApproveContract, ProposalApproveContract.class);
      transactionMap.put(ContractType.ProposalDeleteContract, ProposalDeleteContract.class);
      transactionMap.put(ContractType.ProposalCreateContract, ProposalCreateContract.class);
      transactionMap.put(ContractType.VoteAssetContract, VoteAssetContract.class);
      transactionMap.put(ContractType.CancelAllUnfreezeV2Contract, BalanceContract.CancelAllUnfreezeV2Contract.class);
    }
  }

  public static JSONObject printTransactionToJSON(Transaction transaction, boolean selfType) {
    initTransactionMap();
    JSONObject jsonTransaction = JSONObject
        .parseObject(JsonFormat.printToString(transaction, selfType));
    JSONArray contracts = new JSONArray();
    transaction.getRawData().getContractList().stream().forEach(contract -> {
      try {
        JSONObject contractJson = null;
        Any contractParameter = contract.getParameter();
        switch (contract.getType()) {
          case CreateSmartContract:
            CreateSmartContract deployContract = contractParameter
                .unpack(CreateSmartContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(deployContract, selfType));
            byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
            byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
            jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
            break;
          default:
            Class clazz = transactionMap.get(contract.getType());
            if (clazz != null) {
              contractJson = JSONObject
                  .parseObject(JsonFormat.printToString(contractParameter.unpack(clazz), selfType));
            } else {
              logger.error(
                  "cannot find ContractType, do not forget add ContractType to transactionMap!!");
            }
            break;
        }

        JSONObject parameter = new JSONObject();
        parameter.put("value", contractJson);
        parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
        JSONObject jsonContract = new JSONObject();
        jsonContract.put("parameter", parameter);
        jsonContract.put("type", contract.getType());
        if (contract.getPermissionId() > 0) {
          jsonContract.put("Permission_id", contract.getPermissionId());
        }
        contracts.add(jsonContract);
      } catch (InvalidProtocolBufferException e) {
        logger.debug("InvalidProtocolBufferException: {}", e.getMessage());
      }
    });

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
    jsonTransaction.put("raw_data_hex", rawDataHex);
    String txID = ByteArray.toHexString(Sha256Hash
        .hash(true,
            transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    jsonTransaction.put("visible",selfType);
    return jsonTransaction;
  }

  /**
   * Note: the contracts of the returned transaction may be empty
   */
  public static Transaction packTransaction(String strTransaction, boolean selfType) {
    initTransactionMap();
    JSONObject jsonTransaction = JSON.parseObject(strTransaction);
    JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
    JSONArray contracts = new JSONArray();
    JSONArray rawContractArray = rawData.getJSONArray("contract");

    String contractType = null;
    for (int i = 0; i < rawContractArray.size(); i++) {
      try {
        JSONObject contract = rawContractArray.getJSONObject(i);
        JSONObject parameter = contract.getJSONObject("parameter");
        contractType = contract.getString("type");
        if (StringUtils.isEmpty(contractType)) {
          logger.debug("no type in the transaction, ignore");
          continue;
        }

        Any any = null;
        Class clazz = transactionMap.get(ContractType.valueOf(contractType));
        if (clazz != null) {
          Constructor<GeneratedMessageV3> constructor = clazz.getDeclaredConstructor();
          constructor.setAccessible(true);
          GeneratedMessageV3 generatedMessageV3 = constructor.newInstance();
          Message.Builder builder = generatedMessageV3.toBuilder();
          JsonFormat.merge(parameter.getJSONObject("value").toJSONString(), builder, selfType);
          any = Any.pack(builder.build());
        } else {
          logger.error(
              "cannot find ContractType, do not forget add ContractType to transactionMap!!");
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put("value", value);
          contract.put("parameter", parameter);
          contracts.add(contract);
        }
      } catch (Exception e) {
        e.printStackTrace();

      }
    }
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    try {
      JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder, selfType);
      return transactionBuilder.build();
    } catch (Exception e) {
      logger.debug(" {}", e.getMessage());
      return null;
    }
  }

  public static String getTransactionSign(String transaction, String priKey,
      boolean visible) {

    byte[] privateKey = ByteArray.fromHexString(priKey);
    try {
      TransactionCapsule trx = new TransactionCapsule(packTransaction(transaction,visible));
      trx.sign(privateKey);
      return printTransactionToJSON(trx.getInstance(),visible).toJSONString();
    } catch (Exception e) {
      logger.error("{}", e);
    }
    return null;
  }



  /**
   * constructor.
   */

  public static Transaction setTimestamp(Transaction transaction) {
    long currentTime = System.currentTimeMillis();//*1000000 + System.nanoTime()%1000000;
    Transaction.Builder builder = transaction.toBuilder();
    org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
        .toBuilder();
    rowBuilder.setTimestamp(currentTime);
    builder.setRawData(rowBuilder.build());
    return builder.build();
  }

  /**
   * constructor.
   */
  /*  public static Transaction setDelaySeconds(Transaction transaction, long delaySeconds) {
    DeferredStage deferredStage = transaction.getRawData().toBuilder()
        .getDeferredStage().toBuilder().setDelaySeconds(delaySeconds)
        .setStage(UNEXECUTEDDEFERREDTRANSACTION).build();
    Transaction.raw rawData = transaction.toBuilder().getRawData()
        .toBuilder().setDeferredStage(deferredStage).build();
    return transaction.toBuilder().setRawData(rawData).build();
  }*/

  /*  *//**
   * constructor.
   *//*
  public static GrpcAPI.TransactionExtention setDelaySecondsToExtension(GrpcAPI
      .TransactionExtention transactionExtention, long delaySeconds) {
    if (delaySeconds == 0) {
      return transactionExtention;
    }
    GrpcAPI.TransactionExtention.Builder builder = transactionExtention.toBuilder();

    Transaction transaction = setDelaySeconds(transactionExtention.getTransaction(), delaySeconds);
    builder.setTransaction(transaction);

    return builder.build();
  }*/
}
