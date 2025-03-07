package tron.trident.block;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.trident.core.transaction.BlockId;
import org.tron.trident.core.utils.ByteArray;
import org.tron.trident.core.utils.Sha256Hash;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;

import static org.tron.trident.core.Constant.FULLNODE_NILE;

@Slf4j
public class blockBaseInfo extends TestBase {

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;

    @Test(enabled = true)
    public void test01getblock() throws Exception {

        Response.BlockExtention blockData = wrapper.getBlock(false);
        logger.info(blockData.toString());
        //logger.info("txTrieRoot: {}",(ByteArray.toHexString(blockData.getBlockHeader().getRawData().getTxTrieRoot().toByteArray())));
        //logger.info("parentHash: {}",(ByteArray.toHexString(blockData.getBlockHeader().getRawData().getParentHash().toByteArray())));
        //logger.info("witness_address: {}",(ByteArray.toHexString(blockData.getBlockHeader().getRawData().getWitnessAddress().toByteArray())));
        //logger.info("witness_signature: {}",(ByteArray.toHexString(blockData.getBlockHeader().getWitnessSignature().toByteArray())));
        long blockNum = blockData.getBlockHeader().getRawData().getNumber();
        String blockId = ByteArray.toHexString(blockData.getBlockid().toByteArray());
        logger.info("blockNum: {}, blockId: {}", blockNum, blockId);
        logger.info(String.valueOf(wrapper.getTransactionCountByBlockNum(blockNum)));

        Assert.assertEquals(wrapper.getBlockByIdOrNum(String.valueOf(blockNum)),wrapper.getBlockByIdOrNum(blockId));
        Assert.assertEquals(wrapper.getBlockByIdOrNum(String.valueOf(blockNum)),wrapper.getBlockByIdOrNum(blockId));


    }
    @Test(enabled = true)
    public void test02getblock() throws Exception {

        Chain.Block blockData =  wrapper.getBlockByIdOrNum(String.valueOf(100));
        logger.info(blockData.toString());
        Assert.assertEquals(blockData.getBlockHeader().getRawData().getNumber(),100);
        Assert.assertEquals(ByteArray.toHexString(blockData.getBlockHeader().getRawData().getWitnessAddress().toByteArray()),"41f40cc0264e9655af1f361c35cee7c954afef5841");

        //the param only can get empty result
        Assert.assertEquals(String.valueOf(wrapper.getBlockByIdOrNum(String.valueOf(80000000))),"");
        Assert.assertEquals(String.valueOf(wrapper.getBlockByIdOrNum("000000000000064906fe06df479c9fd016bc3a772360170dc6de983f455b5ba5")),"");

        //invalid param
        try {
            logger.info(String.valueOf(wrapper.getBlockByIdOrNum("0000000000000000d698d4192c4&*b6be724a558448e2684802de4d6cd8690dc")));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith("Invalid blockIDOrNum"));
        }

        //normal block number
        Assert.assertEquals(wrapper.getTransactionCountByBlockNum(52870391),4);
        //not exist block number,much bigger than now block number
        Assert.assertEquals(wrapper.getTransactionCountByBlockNum(80000000),-1);

    }



    @Test(enabled = true)
    public void test11refBlockAndExpireTime() throws Exception {

        String blockIdStr = ByteArray.toHexString(wrapper.getBlockByNum(wrapper.getBlock(false).getBlockHeader().getRawData().getNumber() - 10000).getBlockid().toByteArray());
        //String blockIdStr = "000000000343fe2a999ff9772cdc41057d97a446cba2f74c800dc233cc7f12e1";
        BlockId blockId = new BlockId(Sha256Hash.wrap(hexStringToBytes(blockIdStr)));
        logger.info("the manual set blockid is: " + blockId);
        wrapper.enableLocalCreate(blockId,System.currentTimeMillis() + 60000L);

        Response.TransactionExtention txnExt = wrapper.transfer(owner,"TAvMRE6SL8aYYyVx5mHkEwBPvgD3FGSQND",100);
        Chain.Transaction transaction = wrapper.signTransaction(txnExt);
        String broadcast = wrapper.broadcastTransaction(transaction);
        logger.info("transaction ref-block-bytes is : " + stest.tron.wallet.common.client.utils.ByteArray.toHexString(txnExt.getTransaction().getRawData().getRefBlockBytes().toByteArray()));
        logger.info("transaction ref-block-hash is : " + stest.tron.wallet.common.client.utils.ByteArray.toHexString(txnExt.getTransaction().getRawData().getRefBlockHash().toByteArray()));
        logger.info("txid: " + stest.tron.wallet.common.client.utils.ByteArray.toHexString(txnExt.getTxid().toByteArray()));

//        wrapper.disableLocalCreate();
//        wrapper.setReferHeadBlockId(null);

    }

    @Test(enabled = true)
    public void test06getblock() throws Exception {

        channelFull = ManagedChannelBuilder.forTarget(FULLNODE_NILE)
                .usePlaintext()
                .build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        GrpcAPI.BlockExtention currentBlock = blockingStubFull.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
        System.out.println(currentBlock);
        System.out.println("-------");
        Response.BlockExtention block = wrapper.getBlock(true);
        System.out.println(block);
        Assert.assertEquals(block.toString(),currentBlock.toString());

    }

    public static byte[] hexStringToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hex String length must be even number ");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            String byteStr = hex.substring(i, i + 2);
            bytes[i / 2] = (byte) Integer.parseInt(byteStr, 16);
        }
        return bytes;
    }
}
