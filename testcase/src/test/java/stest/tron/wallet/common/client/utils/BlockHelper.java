package stest.tron.wallet.common.client.utils;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.BalanceContract;
import stest.tron.wallet.common.client.utils.BlockCapsule.BlockId;

@Slf4j
/** Helper for block operations: queries by number, ID, latest block, and block range. */
public class BlockHelper {

  /** Get a block by its number from the full node. */
  public static Protocol.Block getBlock(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());
  }

  /** Get a block extension by its number from the full node. */
  public static BlockExtention getBlock2(long blockNum, WalletBlockingStub blockingStubFull) {
    GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum2(builder.build());
  }

  /** Wait for the solidity node to sync with the full node data. */
  public static boolean waitSolidityNodeSynFullNodeData(
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Block solidityCurrentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Integer wait = 0;
    long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    logger.info("start sync soliditynode, SR number: " + PublicMethod.getWitnessNum(blockingStubFull));
    while (solidityCurrentBlock.getBlockHeader().getRawData().getNumber()
            <= currentBlockNum + 1
        && wait
            < ((PublicMethod.getWitnessNum(blockingStubFull) >= 27)
                ? 27
                : PublicMethod.getWitnessNum(blockingStubFull) + 4)) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      solidityCurrentBlock =
          blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      if (wait == 24) {
        logger.info("Didn't syn,skip to next case.");
        return false;
      }
      wait++;
    }
    logger.info("Fullnode number: " + currentBlockNum
    + ", solidity node number: " + solidityCurrentBlock.getBlockHeader().getRawData().getNumber());

    return true;
  }

  /** Wait until the next block is produced on the full node. */
  public static boolean waitProduceNextBlock(WalletGrpc.WalletBlockingStub blockingStubFull) {
    if (blockingStubFull == null) {
      logger.warn("blockingStubFull is null, cannot wait for next block");
      return false;
    }
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();

    Block nextBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long nextNum = nextBlock.getBlockHeader().getRawData().getNumber();

    Integer wait = 0;
    logger.info("start wait produce block, current num: " + currentBlock.getBlockHeader().getRawData().getNumber());
    while (nextNum <= currentNum + 1 && wait <= 45) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      nextBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      nextNum = nextBlock.getBlockHeader().getRawData().getNumber();
      if (wait == 45) {
        logger.info("quit timeout, These 45 second didn't produce a block,please check.");
        return false;
      }
      wait++;
    }
    logger.info("quit normally, wait times: " + wait);
    return true;
  }

  /** Wait until transaction info is found by txId, or timeout. */
  public static void waitUntilTransactionInfoFound(WalletGrpc.WalletBlockingStub blockingStubFull, String txId, int timeout) {
    Integer wait = 0;
    while (wait++ <= timeout) {
      try {
        // wait 3 seconds
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txId, blockingStubFull);
      if(infoById.get().getBlockTimeStamp() > 0){
        logger.info("quit normally, wait tx by id: " + txId + " times: " + wait);
        return;
      }
    }
    logger.info("quit timeout, wait tx by id: " + txId + " times: " + wait);

  }

  /** Get transaction info list by block number from the full node. */
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNum(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    TransactionInfoList transactionInfoList;
    transactionInfoList = blockingStubFull.getTransactionInfoByBlockNum(builder.build());
    return Optional.ofNullable(transactionInfoList);
  }

  /** Get transaction info list by block number from the solidity node. */
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNumFromSolidity(
      long blockNum, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    TransactionInfoList transactionInfoList;
    transactionInfoList = blockingStubSolidity.getTransactionInfoByBlockNum(builder.build());
    return Optional.ofNullable(transactionInfoList);
  }

  /** Get block balance trace for a given block. */
  public static BalanceContract.BlockBalanceTrace getBlockBalance(
      Protocol.Block block, WalletGrpc.WalletBlockingStub blockingStubFull) {
    final Long blockNum = block.getBlockHeader().getRawData().getNumber();

    BlockId blockId =
        new BlockId(
            Sha256Hash.of(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                block.getBlockHeader().getRawData().toByteArray()),
            block.getBlockHeader().getRawData().getNumber());
    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setHash(blockId.getByteString())
            .setNumber(blockNum)
            .build();

    return blockingStubFull.getBlockBalanceTrace(blockIdentifier);
  }
}
