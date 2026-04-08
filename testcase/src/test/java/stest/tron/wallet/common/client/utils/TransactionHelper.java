package stest.tron.wallet.common.client.utils;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;

/** Utility class for transaction-related operations such as signing, broadcasting, and querying. */
@Slf4j
public class TransactionHelper {

  /** Sign a transaction using the given ECKey. */
  public static Protocol.Transaction signTransaction(
      ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      // logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /** Sign a shielded transaction using the given ECKey without resetting timestamp. */
  public static Protocol.Transaction signTransactionForShield(
      ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      // logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    return TransactionUtils.sign(transaction, ecKey);
  }

  /** Query a transaction by its ID using a solidity blocking stub. */
  public static Optional<Transaction> getTransactionById(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.getTransactionById(request);

    return Optional.ofNullable(transaction);
  }

  /** Query a transaction by its ID using a wallet blocking stub. */
  public static Optional<Transaction> getTransactionById(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.getTransactionById(request);
    return Optional.ofNullable(transaction);
  }

  /** Query a transaction by its ID from the solidity node. */
  public static Optional<Transaction> getTransactionByIdSolidity(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubSolidity.getTransactionById(request);
    return Optional.ofNullable(transaction);
  }

  /** Format a transaction as a human-readable string with hash, txid, and raw data. */
  public static String printTransaction(Transaction transaction) {
    String result = "";
    result += "hash: ";
    result += "\n";
    result +=
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(), transaction.toByteArray()));
    result += "\n";
    result += "txid: ";
    result += "\n";
    result +=
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
    result += "\n";

    if (transaction.getRawData() != null) {
      result += "raw_data: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printTransactionRow(transaction.getRawData());
      result += "}";
      result += "\n";
    }

    return result;
  }

  /** Extract the timestamp from a transaction's raw data. */
  public static long printTransactionRow(Transaction.raw raw) {
    long timestamp = raw.getTimestamp();

    return timestamp;
  }

  /** Query transaction info by transaction ID from a full node. */
  public static Optional<TransactionInfo> getTransactionInfoById(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;
    transactionInfo = blockingStubFull.getTransactionInfoById(request);
    return Optional.ofNullable(transactionInfo);
  }

  /** Query transaction info by transaction ID from a solidity node. */
  public static Optional<TransactionInfo> getTransactionInfoByIdFromSolidity(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;
    transactionInfo = blockingStubFull.getTransactionInfoById(request);
    return Optional.ofNullable(transactionInfo);
  }

  /** Query all transaction info for a given block number from a full node. */
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNum(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    TransactionInfoList transactionInfoList;
    transactionInfoList = blockingStubFull.getTransactionInfoByBlockNum(builder.build());
    return Optional.ofNullable(transactionInfoList);
  }

  /** Query all transaction info for a given block number from a solidity node. */
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNumFromSolidity(
      long blockNum, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    TransactionInfoList transactionInfoList;
    transactionInfoList = blockingStubSolidity.getTransactionInfoByBlockNum(builder.build());
    return Optional.ofNullable(transactionInfoList);
  }

  /** Add an additional signature to an existing transaction. */
  public static Transaction addTransactionSign(
      Transaction transaction, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash =
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray());

    ECKey.ECDSASignature signature = ecKey.sign(hash);
    ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
    transactionBuilderSigned.addSignature(bsSign);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  /** Get the list of accounts that have approved (signed) a transaction. */
  public static TransactionApprovedList getTransactionApprovedList(
      Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getTransactionApprovedList(transaction);
  }

  /** Broadcast a signed transaction to the network, retrying on SERVER_BUSY. */
  public static GrpcAPI.Return broadcastTransaction(
      Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    String txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    logger.info("broadcastTransaction: " + txid);
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    while (!response.getResult() && response.getCode() == response_code.SERVER_BUSY && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
      logger.info("repeate times = " + (10 - i));
    }

    if (response.getResult() == false) {
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
    }
    return response;
  }

  /** Broadcast a transaction to two full nodes simultaneously, retrying on SERVER_BUSY. */
  public static GrpcAPI.Return broadcastTransactionBoth(
      Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletGrpc.WalletBlockingStub blockingStubFull1) {
    int i = 10;
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    GrpcAPI.Return response = blockingStubFull1.broadcastTransaction(transaction);
    GrpcAPI.Return response1 = blockingStubFull.broadcastTransaction(transaction);
    while (response.getResult() == false
        && response.getCode() == response_code.SERVER_BUSY
        && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
      logger.info("repeate times = " + (10 - i));
    }

    if (response.getResult() == false) {
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
    }
    return response;
  }
}
