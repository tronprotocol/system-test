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

package stest.tron.wallet.common.client.utils;


import com.google.common.primitives.Bytes;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Permission.PermissionType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.Transaction.raw;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import org.tron.protos.contract.WitnessContract.WitnessCreateContract;
import org.tron.protos.contract.WitnessContract.WitnessUpdateContract;
import stest.tron.wallet.common.client.utils.exception.BadItemException;

@Slf4j(topic = "capsule")
public class TransactionCapsule implements ProtoCapsule<Transaction> {

  private static final ExecutorService executorService = Executors
      .newFixedThreadPool(CommonParameter.getInstance()
          .getValidContractProtoThreadNum());
  private static final String OWNER_ADDRESS = "ownerAddress_";

  private Transaction transaction;
  @Setter
  private boolean isVerified = false;
  @Setter
  @Getter
  private long blockNum = -1;


  private StringBuilder toStringBuff = new StringBuilder();
  @Getter
  @Setter
  private long time;
  @Getter
  @Setter
  private long order;
  private byte[] ownerAddress;
  private Sha256Hash id;

  @Getter
  @Setter
  private boolean isTransactionCreate = false;



  /**
   * constructor TransactionCapsule.
   */
  public TransactionCapsule(Transaction trx) {
    this.transaction = trx;
  }


  public TransactionCapsule(VoteWitnessContract voteWitnessContract) {
    createTransaction(voteWitnessContract, ContractType.VoteWitnessContract);
  }

  public TransactionCapsule(WitnessCreateContract witnessCreateContract) {
    createTransaction(witnessCreateContract, ContractType.WitnessCreateContract);
  }

  public TransactionCapsule(WitnessUpdateContract witnessUpdateContract) {
    createTransaction(witnessUpdateContract, ContractType.WitnessUpdateContract);
  }

  public TransactionCapsule(TransferAssetContract transferAssetContract) {
    createTransaction(transferAssetContract, ContractType.TransferAssetContract);
  }

  public TransactionCapsule(ParticipateAssetIssueContract participateAssetIssueContract) {
    createTransaction(participateAssetIssueContract, ContractType.ParticipateAssetIssueContract);
  }

  public TransactionCapsule(raw rawData, List<ByteString> signatureList) {
    this.transaction = Transaction.newBuilder().setRawData(rawData).addAllSignature(signatureList)
        .build();
  }

  @Deprecated
  public TransactionCapsule(AssetIssueContract assetIssueContract) {
    createTransaction(assetIssueContract, ContractType.AssetIssueContract);
  }

  public TransactionCapsule(com.google.protobuf.Message message, ContractType contractType) {
    raw.Builder transactionBuilder = raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            (message instanceof Any ? (Any) message : Any.pack(message))).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public static long getWeight(Permission permission, byte[] address) {
    List<Key> list = permission.getKeysList();
    for (Key key : list) {
      if (key.getAddress().equals(ByteString.copyFrom(address))) {
        return key.getWeight();
      }
    }
    return 0;
  }


  public static <T extends com.google.protobuf.Message> T parse(Class<T> clazz,
      CodedInputStream codedInputStream) throws InvalidProtocolBufferException {
    T defaultInstance = Internal.getDefaultInstance(clazz);
    return (T) defaultInstance.getParserForType().parseFrom(codedInputStream);
  }


  // todo mv this static function to capsule util
  public static byte[] getToAddress(Transaction.Contract contract) {
    ByteString to;
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TransferContract:
          to = contractParameter.unpack(TransferContract.class).getToAddress();
          break;
        case TransferAssetContract:
          to = contractParameter.unpack(TransferAssetContract.class).getToAddress();
          break;
        case ParticipateAssetIssueContract:
          to = contractParameter.unpack(ParticipateAssetIssueContract.class).getToAddress();
          break;

        default:
          return new byte[0];
      }
      return to.toByteArray();
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return new byte[0];
    }
  }

  // todo mv this static function to capsule util
  public static long getCallValue(Transaction.Contract contract) {
    try {
      Any contractParameter = contract.getParameter();
      switch (contract.getType()) {
        case TriggerSmartContract:
          return contractParameter.unpack(TriggerSmartContract.class).getCallValue();

        case CreateSmartContract:
          return contractParameter.unpack(CreateSmartContract.class).getNewContract()
              .getCallValue();
        default:
          return 0L;
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage());
      return 0L;
    }
  }


  public void resetResult() {
    if (this.getInstance().getRetCount() > 0) {
      this.transaction = this.getInstance().toBuilder().clearRet().build();
    }
  }


  public void setReference(long blockNum, byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    raw rawData = this.transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(ByteArray.subArray(blockHash, 8, 16)))
        .setRefBlockBytes(ByteString.copyFrom(ByteArray.subArray(refBlockNum, 6, 8)))
        .build();
    setRawData(rawData);
  }

  public long getExpiration() {
    return transaction.getRawData().getExpiration();
  }

  /**
   * @param expiration must be in milliseconds format
   */
  public void setExpiration(long expiration) {
    raw rawData = this.transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    setRawData(rawData);
  }

  public void setTimestamp() {
    raw rawData = this.transaction.getRawData().toBuilder()
        .setTimestamp(System.currentTimeMillis())
        .build();
    setRawData(rawData);
  }

  public void setTimestamp(long timestamp) {
    raw rawData = this.transaction.getRawData().toBuilder()
        .setTimestamp(timestamp)
        .build();
    setRawData(rawData);
  }

  public long getTimestamp() {
    return transaction.getRawData().getTimestamp();
  }

  public void setFeeLimit(long feeLimit) {
    raw rawData = this.transaction.getRawData().toBuilder()
        .setFeeLimit(feeLimit)
        .build();
    setRawData(rawData);
  }

  public long getFeeLimit() {
    return transaction.getRawData().getFeeLimit();
  }

  @Deprecated
  public void createTransaction(com.google.protobuf.Message message, ContractType contractType) {
    raw.Builder transactionBuilder = raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());
    transaction = Transaction.newBuilder().setRawData(transactionBuilder.build()).build();
  }

  public Sha256Hash getMerkleHash() {
    byte[] transBytes = this.transaction.toByteArray();
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        transBytes);
  }

  private Sha256Hash getRawHash() {
    return Sha256Hash.of(CommonParameter.getInstance().isECKeyCryptoEngine(),
        this.transaction.getRawData().toByteArray());
  }

  public static SignInterface fromPrivate(byte[] privKeyBytes, boolean isECKeyCryptoEngine) {
    if (isECKeyCryptoEngine) {
      return ECKey.fromPrivate(privKeyBytes);
    }
    return null;
  }

  public void sign(byte[] privateKey) {
    SignInterface cryptoEngine = fromPrivate(privateKey, true);
    ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
        .signHash(getTransactionId().getBytes())));
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }

  public void addSign(byte[] privateKey)
      throws  SignatureException {
    Transaction.Contract contract = this.transaction.getRawData().getContract(0);

    SignInterface cryptoEngine = fromPrivate(privateKey, true);

    ByteString sig = ByteString.copyFrom(cryptoEngine.Base64toBytes(cryptoEngine
        .signHash(getTransactionId().getBytes())));
    this.transaction = this.transaction.toBuilder().addSignature(sig).build();
  }
  

  public Sha256Hash getTransactionId() {
    if (this.id == null) {
      this.id = getRawHash();
    }
    return this.id;
  }

  private void setRawData(raw rawData) {
    this.transaction = this.transaction.toBuilder().setRawData(rawData).build();
    // invalidate trxId
    this.id = null;
  }

  @Override
  public byte[] getData() {
    return this.transaction.toByteArray();
  }

  public long getSerializedSize() {
    return this.transaction.getSerializedSize();
  }

  /**
   * Compute the number of bytes that would be needed to encode an embedded message field, including
   * tag.
   * message Block {
   *   repeated Transaction transactions = 1;
   *   BlockHeader block_header = 2;
   * }
   */
  public long computeTrxSizeForBlockMessage() {
    return CodedOutputStream.computeMessageSize(1, this.transaction);
  }

  public long getResultSerializedSize() {
    long size = 0;
    for (Result result : this.transaction.getRetList()) {
      size += result.getSerializedSize();
    }
    return size;
  }

  @Override
  public Transaction getInstance() {
    return this.transaction;
  }


  public void setResultCode(contractResult code) {
    Result ret;
    if (this.transaction.getRetCount() > 0) {
      ret = this.transaction.getRet(0).toBuilder().setContractRet(code).build();

      this.transaction = transaction.toBuilder().setRet(0, ret).build();
      return;
    }
    ret = Result.newBuilder().setContractRet(code).build();
    this.transaction = transaction.toBuilder().addRet(ret).build();
  }

  public contractResult getContractResult() {
    if (this.transaction.getRetCount() > 0) {
      return this.transaction.getRet(0).getContractRet();
    }
    return null;
  }



  public contractResult getContractRet() {
    if (this.transaction.getRetCount() <= 0) {
      return null;
    }
    return this.transaction.getRet(0).getContractRet();
  }

  /**
   * Check if a transaction capsule contains a smart contract transaction or not.
   * @return
   */
  public boolean isContractType() {
    try {
      ContractType type = this.getInstance().getRawData().getContract(0).getType();
      return  (type == ContractType.TriggerSmartContract || type == ContractType.CreateSmartContract);
    } catch (Exception ex) {
      logger.warn("check contract type failed, reason {}", ex.getMessage());
      return false;
    }
  }

  public TransferContract getTransferContract() {
    try {
      return transaction.getRawData()
          .getContract(0)
          .getParameter()
          .unpack(TransferContract.class);
    } catch (InvalidProtocolBufferException e) {
      return null;
    }
  }
}
