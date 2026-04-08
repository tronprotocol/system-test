package stest.tron.wallet.common.client.utils;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.netty.util.internal.StringUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DecryptNotes.NoteTx;
import org.tron.api.GrpcAPI.DecryptNotesMarked;
import org.tron.api.GrpcAPI.IvkDecryptAndMarkParameters;
import org.tron.api.GrpcAPI.IvkDecryptParameters;
import org.tron.api.GrpcAPI.NfParameters;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.NoteParameters;
import org.tron.api.GrpcAPI.OvkDecryptParameters;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.api.GrpcAPI.PrivateParametersWithoutAsk;
import org.tron.api.GrpcAPI.ReceiveNote;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.api.GrpcAPI.SpendNote;
import org.tron.api.GrpcAPI.SpendResult;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;
import stest.tron.wallet.common.client.utils.zen.address.DiversifierT;
import stest.tron.wallet.common.client.utils.zen.address.ExpandedSpendingKey;
import stest.tron.wallet.common.client.utils.zen.address.FullViewingKey;
import stest.tron.wallet.common.client.utils.zen.address.IncomingViewingKey;
import stest.tron.wallet.common.client.utils.zen.address.PaymentAddress;
import stest.tron.wallet.common.client.utils.zen.address.SpendingKey;

@Slf4j
/** Helper for shielded/privacy operations: note management, shield transfers, and zero-knowledge proofs. */
public class ShieldHelper {

  public static Map<Long, ShieldNoteInfo> utxoMapNote = new ConcurrentHashMap();
  public static List<ShieldNoteInfo> spendUtxoList = new ArrayList<>();

  /** Signs a transaction using the provided ECKey for shielded transfers. */
  public static Protocol.Transaction signTransactionForShield(
      ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      // logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    return TransactionUtils.sign(transaction, ecKey);
  }

  /** Sends a shielded coin transaction with full spend authority. */
  public static boolean sendShieldCoin(
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
      String priKey,
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

    PrivateParameters.Builder builder = PrivateParameters.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      // String shieldAddress = noteInfo.getPaymentAddress();
      // ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        builder.setAsk(ByteString.copyFrom(expandedSpendingKey.getAsk()));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      // System.out.println("address " + noteInfo.getPaymentAddress());
      // System.out.println("value " + noteInfo.getValue());
      // System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      // System.out.println("trxId " + noteInfo.getTrxId());
      // System.out.println("index " + noteInfo.getIndex());
      // System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(
            ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      IncrementalMerkleVoucherInfo merkleVoucherInfo =
          blockingStubFull.getMerkleTreeVoucherInfo(request.build());
      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk =
          ByteArray.fromHexString(
              "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder.addShieldedReceives(
            ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention =
        blockingStubFull.createShieldedTransaction(builder.build());
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
    Any any = transaction.getRawData().getContract(0).getParameter();

    try {
      ShieldedTransferContract shieldedTransferContract =
          any.unpack(ShieldedTransferContract.class);
      if (shieldedTransferContract.getFromAmount() > 0 || fromAmount == 321321) {
        transaction = signTransactionForShield(ecKey, transaction);
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      } else {
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    return PublicMethod.broadcastTransaction(transaction, blockingStubFull).getResult();
  }

  /** Sends a shielded coin transaction without the spend authority key (uses separate auth sig). */
  public static boolean sendShieldCoinWithoutAsk(
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
      String priKey,
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

    PrivateParametersWithoutAsk.Builder builder = PrivateParametersWithoutAsk.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    byte[] ask = new byte[32];
    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());
      IncrementalMerkleVoucherInfo merkleVoucherInfo =
          blockingStubFull.getMerkleTreeVoucherInfo(request.build());
      if (merkleVoucherInfo.getVouchersCount() != 1) {
        System.out.println("Can't get all merkel tree, please check the notes.");
        return false;
      }

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      // String shieldAddress = noteInfo.getPaymentAddress();
      // ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      String shieldAddress = noteTx.getNote().getPaymentAddress();
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        System.arraycopy(expandedSpendingKey.getAsk(), 0, ask, 0, 32);
        builder.setAk(
            ByteString.copyFrom(ExpandedSpendingKey.getAkFromAsk(expandedSpendingKey.getAsk())));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      // System.out.println("address " + noteInfo.getPaymentAddress());
      // System.out.println("value " + noteInfo.getValue());
      // System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      // System.out.println("trxId " + noteInfo.getTrxId());
      // System.out.println("index " + noteInfo.getIndex());
      // System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(
            ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk =
          ByteArray.fromHexString(
              "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder.addShieldedReceives(
            ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention =
        blockingStubFull.createShieldedTransactionWithoutSpendAuthSig(builder.build());
    if (transactionExtention == null) {
      System.out.println("sendShieldCoinWithoutAsk failure.");
      return false;
    }
    BytesMessage trxHash =
        blockingStubFull.getShieldTransactionHash(transactionExtention.getTransaction());
    if (trxHash == null || trxHash.getValue().toByteArray().length != 32) {
      System.out.println("sendShieldCoinWithoutAsk get transaction hash failure.");
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRawData().getContract(0).getType()
        != ContractType.ShieldedTransferContract) {
      System.out.println("This method only for ShieldedTransferContract, please check!");
      return false;
    }
    Any any = transaction.getRawData().getContract(0).getParameter();
    Transaction transaction1 = transactionExtention.getTransaction();
    try {
      ShieldedTransferContract shieldContract = any.unpack(ShieldedTransferContract.class);
      List<SpendDescription> spendDescList = shieldContract.getSpendDescriptionList();
      ShieldedTransferContract.Builder contractBuild =
          shieldContract.toBuilder().clearSpendDescription();
      for (int i = 0; i < spendDescList.size(); i++) {

        SpendAuthSigParameters.Builder builder1 = SpendAuthSigParameters.newBuilder();
        builder1.setAsk(ByteString.copyFrom(ask));
        builder1.setTxHash(ByteString.copyFrom(trxHash.getValue().toByteArray()));
        builder1.setAlpha(builder.getShieldedSpends(i).getAlpha());
        SpendDescription.Builder spendDescription = spendDescList.get(i).toBuilder();
        BytesMessage authSig = blockingStubFull.createSpendAuthSig(builder1.build());
        spendDescription.setSpendAuthoritySignature(
            ByteString.copyFrom(authSig.getValue().toByteArray()));

        contractBuild.addSpendDescription(spendDescription.build());
      }

      Transaction.raw.Builder rawBuilder =
          transaction.toBuilder()
              .getRawDataBuilder()
              .clearContract()
              .addContract(
                  Transaction.Contract.newBuilder()
                      .setType(ContractType.ShieldedTransferContract)
                      .setParameter(Any.pack(contractBuild.build()))
                      .build());

      transaction = transaction.toBuilder().clearRawData().setRawData(rawBuilder).build();

      transactionExtention = transactionExtention.toBuilder().setTransaction(transaction).build();

      if (transactionExtention == null) {
        return false;
      }
      Return ret = transactionExtention.getResult();
      if (!ret.getResult()) {
        System.out.println("Code = " + ret.getCode());
        System.out.println("Message = " + ret.getMessage().toStringUtf8());
        return false;
      }
      transaction1 = transactionExtention.getTransaction();
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        System.out.println("Transaction is empty");
        return false;
      }
      System.out.println(
          "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

      if (transaction1.getRawData().getContract(0).getType()
          != ContractType.ShieldedTransferContract) {
        transaction1 = PublicMethod.signTransaction(ecKey, transaction1);
      } else {
        Any any1 = transaction1.getRawData().getContract(0).getParameter();
        ShieldedTransferContract shieldedTransferContract =
            any1.unpack(ShieldedTransferContract.class);
        if (shieldedTransferContract.getFromAmount() > 0) {
          transaction1 = signTransactionForShield(ecKey, transaction1);
          System.out.println(
              "trigger txid = "
                  + ByteArray.toHexString(
                      Sha256Hash.hash(
                          CommonParameter.getInstance().isECKeyCryptoEngine(),
                          transaction1.getRawData().toByteArray())));
        }
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction1.getRawData().toByteArray())));
    return PublicMethod.broadcastTransaction(transaction1, blockingStubFull).getResult();
  }

  /** Builds a shielded output note and adds it to the output list. */
  public static List<Note> addShieldOutputList(
      List<Note> shieldOutList, String shieldToAddress, String toAmountString, String menoString) {
    String shieldAddress = shieldToAddress;
    String amountString = toAmountString;
    if (menoString.equals("null")) {
      menoString = "";
    }
    long shieldAmount = 0;
    if (!StringUtil.isNullOrEmpty(amountString)) {
      shieldAmount = Long.valueOf(amountString);
    }

    Note.Builder noteBuild = Note.newBuilder();
    noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setValue(shieldAmount);
    try {
      noteBuild.setRcm(
          ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
    } catch (Exception e) {
      System.out.println(e);
    }
    noteBuild.setMemo(ByteString.copyFrom(menoString.getBytes()));
    shieldOutList.add(noteBuild.build());
    // logger.info(shieldOutList.toString());
    return shieldOutList;
  }

  /** Generates a new random shielded payment address with spending and viewing keys. */
  public static Optional<ShieldAddressInfo> generateShieldAddress() {
    ShieldAddressInfo addressInfo = new ShieldAddressInfo();
    try {
      DiversifierT diversifier = DiversifierT.random();
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(diversifier).get();

      addressInfo.setSk(spendingKey.getValue());
      addressInfo.setD(diversifier);
      addressInfo.setIvk(incomingViewingKey.getValue());
      addressInfo.setOvk(fullViewingKey.getOvk());
      addressInfo.setPkD(paymentAddress.getPkD());

      if (addressInfo.validateCheck()) {
        return Optional.of(addressInfo);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  /** Lists shielded notes by scanning recent blocks using the incoming viewing key. */
  public static DecryptNotes listShieldNote(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    logger.info(ByteArray.toHexString(shieldAddressInfo.get().ivk));
    IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum);
    builder.setEndBlockIndex(currentBlockNum + 1);
    builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
    DecryptNotes notes = blockingStubFull.scanNoteByIvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** Scans shielded notes by incoming viewing key from recent blocks. */
  public static DecryptNotes getShieldNotesByIvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    // startBlockNum = 0L;
    logger.info("ivk:" + ByteArray.toHexString(shieldAddressInfo.get().ivk));
    IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum + 1);
    builder.setEndBlockIndex(currentBlockNum + 1);
    builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
    DecryptNotes notes = blockingStubFull.scanNoteByIvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** Scans and marks shielded notes by incoming viewing key, indicating spent status. */
  public static DecryptNotesMarked getShieldNotesAndMarkByIvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    // startBlockNum = 0L;
    logger.info("ivk:" + ByteArray.toHexString(shieldAddressInfo.get().ivk));
    try {
      IvkDecryptAndMarkParameters.Builder builder = IvkDecryptAndMarkParameters.newBuilder();
      builder.setStartBlockIndex(startBlockNum + 1);
      builder.setEndBlockIndex(currentBlockNum + 1);
      builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
      builder.setAk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getAk()));
      builder.setNk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getNk()));
      DecryptNotesMarked decryptNotes = blockingStubFull.scanAndMarkNoteByIvk(builder.build());
      logger.info(decryptNotes.toString());
      return decryptNotes;
    } catch (Exception e) {
      logger.info(e.toString());
      return null;
    }
  }

  /** Scans and marks shielded notes by incoming viewing key on the solidity node. */
  public static DecryptNotesMarked getShieldNotesAndMarkByIvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    // startBlockNum = 0L;
    logger.info("ivk:" + ByteArray.toHexString(shieldAddressInfo.get().ivk));
    try {
      IvkDecryptAndMarkParameters.Builder builder = IvkDecryptAndMarkParameters.newBuilder();
      builder.setStartBlockIndex(startBlockNum + 1);
      builder.setEndBlockIndex(currentBlockNum + 1);
      builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
      builder.setAk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getAk()));
      builder.setNk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getNk()));
      DecryptNotesMarked decryptNotes = blockingStubSolidity.scanAndMarkNoteByIvk(builder.build());
      logger.info(decryptNotes.toString());
      return decryptNotes;
    } catch (Exception e) {
      logger.info(e.toString());
      return null;
    }
  }

  /** Scans shielded notes by incoming viewing key on the solidity node. */
  public static DecryptNotes getShieldNotesByIvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum);
    builder.setEndBlockIndex(currentBlockNum);
    builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
    DecryptNotes notes = blockingStubSolidity.scanNoteByIvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** Scans shielded notes by outgoing viewing key from recent blocks. */
  public static DecryptNotes getShieldNotesByOvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    logger.info("ovk:" + ByteArray.toHexString(shieldAddressInfo.get().ovk));
    OvkDecryptParameters.Builder builder = OvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum + 1);
    builder.setEndBlockIndex(currentBlockNum + 1);
    builder.setOvk(ByteString.copyFrom(shieldAddressInfo.get().getOvk()));
    DecryptNotes notes = blockingStubFull.scanNoteByOvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** Scans shielded notes by outgoing viewing key on the solidity node. */
  public static DecryptNotes getShieldNotesByOvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    OvkDecryptParameters.Builder builder = OvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum);
    builder.setEndBlockIndex(currentBlockNum);
    builder.setOvk(ByteString.copyFrom(shieldAddressInfo.get().getOvk()));
    DecryptNotes notes = blockingStubSolidity.scanNoteByOvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** Extracts the memo string from a shielded note. */
  public static String getMemo(Note note) {
    return ZenUtils.getMemo(note.getMemo().toByteArray());
  }

  /** Checks whether a shielded note has been spent on the full node. */
  public static SpendResult getSpendResult(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
    outPointBuild.setIndex(noteTx.getIndex());
    request.addOutPoints(outPointBuild.build());
    Optional<IncrementalMerkleVoucherInfo> merkleVoucherInfo =
        Optional.of(blockingStubFull.getMerkleTreeVoucherInfo(request.build()));

    if (merkleVoucherInfo.isPresent() && merkleVoucherInfo.get().getVouchersCount() > 0) {
      NoteParameters.Builder builder = NoteParameters.newBuilder();
      try {
        builder.setAk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getAk()));
        builder.setNk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getNk()));
        logger.info("AK:" + ByteArray.toHexString(shieldAddressInfo.getFullViewingKey().getAk()));
        logger.info("NK:" + ByteArray.toHexString(shieldAddressInfo.getFullViewingKey().getNk()));
      } catch (Exception e) {
        Assert.assertTrue(1 == 1);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));
      builder.setNote(noteBuild.build());
      builder.setTxid(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      builder.setIndex(noteTx.getIndex());
      // builder.setVoucher(merkleVoucherInfo.getVouchers(0));

      SpendResult result = blockingStubFull.isSpend(builder.build());
      return result;
    }
    return null;
  }

  /** Checks whether a shielded note has been spent on the solidity node. */
  public static SpendResult getSpendResultOnSolidity(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
    outPointBuild.setIndex(noteTx.getIndex());
    request.addOutPoints(outPointBuild.build());
    Optional<IncrementalMerkleVoucherInfo> merkleVoucherInfo =
        Optional.of(blockingStubSolidity.getMerkleTreeVoucherInfo(request.build()));

    if (merkleVoucherInfo.isPresent() && merkleVoucherInfo.get().getVouchersCount() > 0) {
      NoteParameters.Builder builder = NoteParameters.newBuilder();
      try {
        builder.setAk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getAk()));
        builder.setNk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getNk()));
      } catch (Exception e) {
        Assert.assertTrue(1 == 1);
      }
      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));
      builder.setNote(noteBuild.build());
      builder.setTxid(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      builder.setIndex(noteTx.getIndex());
      // builder.setVoucher(merkleVoucherInfo.getVouchers(0));

      SpendResult result = blockingStubSolidity.isSpend(builder.build());
      return result;
    }
    return null;
  }

  /** Computes the nullifier for a shielded note using the full viewing key. */
  public static String getShieldNullifier(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
    outPointBuild.setIndex(noteTx.getIndex());
    request.addOutPoints(outPointBuild.build());
    IncrementalMerkleVoucherInfo merkleVoucherInfo =
        blockingStubFull.getMerkleTreeVoucherInfo(request.build());
    if (merkleVoucherInfo.getVouchersCount() < 1) {
      System.out.println("get merkleVoucherInfo failure.");
      return null;
    }
    Note.Builder noteBuild = Note.newBuilder();
    noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
    noteBuild.setValue(noteTx.getNote().getValue());
    noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
    noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

    String shieldAddress = noteTx.getNote().getPaymentAddress();
    SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
    try {
      // TODO
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      NfParameters.Builder builder = NfParameters.newBuilder();
      builder.setNote(noteBuild.build());
      builder.setVoucher(merkleVoucherInfo.getVouchers(0));
      builder.setAk(ByteString.copyFrom(fullViewingKey.getAk()));
      builder.setNk(ByteString.copyFrom(fullViewingKey.getNk()));

      BytesMessage nullifier = blockingStubFull.createShieldNullifier(builder.build());
      return ByteArray.toHexString(nullifier.getValue().toByteArray());

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /** Sends a shielded coin transaction and returns the transaction ID. */
  public static String sendShieldCoinGetTxid(
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
      String priKey,
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

    PrivateParameters.Builder builder = PrivateParameters.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      // String shieldAddress = noteInfo.getPaymentAddress();
      // ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        builder.setAsk(ByteString.copyFrom(expandedSpendingKey.getAsk()));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      // System.out.println("address " + noteInfo.getPaymentAddress());
      // System.out.println("value " + noteInfo.getValue());
      // System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      // System.out.println("trxId " + noteInfo.getTrxId());
      // System.out.println("index " + noteInfo.getIndex());
      // System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(
            ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      IncrementalMerkleVoucherInfo merkleVoucherInfo =
          blockingStubFull.getMerkleTreeVoucherInfo(request.build());
      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk =
          ByteArray.fromHexString(
              "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder.addShieldedReceives(
            ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention =
        blockingStubFull.createShieldedTransaction(builder.build());
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    Any any = transaction.getRawData().getContract(0).getParameter();

    try {
      ShieldedTransferContract shieldedTransferContract =
          any.unpack(ShieldedTransferContract.class);
      if (shieldedTransferContract.getFromAmount() > 0) {
        transaction = signTransactionForShield(ecKey, transaction);
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      } else {
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
  }
}
