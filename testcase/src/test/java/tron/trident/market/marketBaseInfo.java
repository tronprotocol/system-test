package tron.trident.market;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import stest.tron.wallet.common.client.utils.ByteArray;
import tron.trident.utils.TestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class marketBaseInfo extends TestBase {

    @Test(enabled = true)
    public void test01getMarketOrderByAccount() throws Exception {

        Response.ExchangeList exchangeList =  wrapper.listExchanges();
        Assert.assertTrue(exchangeList.getExchangesCount() >= 63);

        //get normal & empty order result
        Assert.assertEquals(wrapper.getMarketOrderByAccount("THyHj1gf2P6u4BY5uQhUHGwxZLXkoFJPZS").getOrdersCount(),0);
        Assert.assertEquals(wrapper.getMarketOrderByAccount("TX74o6dWugAgdaMv8M39QP9YL5QRgfj32t").getOrdersCount(),5);
        //invalid param
        try {
            logger.info(wrapper.getMarketOrderByAccount("TAvMRE6SL8aYYyVx5mHkEwBPvgD3FGSQN").toString());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Checksum mismatch");
        }
        try {
            logger.info(wrapper.getMarketOrderByAccount("TAvMRE6SL8aYYyVx5mHkEwBPv*gD3FGSQN").toString());
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Invalid character for Base58Check");
        }
    }
    @Test(enabled = true)
    public void test02getMarketOrderById() throws Exception {

        //query order id normal & not exist
        ByteString tmp = wrapper.getMarketOrderByAccount("TX74o6dWugAgdaMv8M39QP9YL5QRgfj32t").getOrders(0).getOrderId();
        String orderId = ByteArray.toHexString(tmp.toByteArray());
        logger.info("orderId: {}", orderId);
        logger.info(wrapper.getMarketOrderById(orderId).toString());
        Assert.assertEquals(ByteArray.toHexString(wrapper.getMarketOrderById(orderId).getOwnerAddress().toByteArray()),"41e7d71e72ea48de9144dc2450e076415af0ea745f");

        List<String> paramList = new ArrayList<>();
        paramList.add("0e0fd7d34561fd4126d56748354cd303cad7846d0f5baf13bc1f824782ed70a");
        paramList.add("100567");
        for (String param : paramList) {
            try {
                logger.info(wrapper.getMarketOrderById(param).toString());
                Assert.fail();
            } catch (StatusRuntimeException e) {
                logger.info(e.getMessage());
                Assert.assertEquals(e.getMessage(), "INTERNAL: order not found in store");
            }
        }
    }

    @Test(enabled = true)
    public void test03getMarketOrderListByPair() throws Exception {
        //result only contains one order pair
        Response.MarketOrder lst = wrapper.getMarketOrderListByPair("1000323", "1000598").getOrders(0);
        Assert.assertEquals(lst.getBuyTokenId().toStringUtf8(), "1000598");
        Assert.assertEquals(lst.getSellTokenId().toStringUtf8(), "1000323");
        //result contains more than one order pair
        Assert.assertEquals(wrapper.getMarketOrderListByPair("1000323", "1000340").getOrdersCount(), 2);
        //query between trc10 token and TRX
        Assert.assertEquals(wrapper.getMarketOrderListByPair("1000323", "_").getOrders(0).getBuyTokenId().toStringUtf8(), "_");
        Assert.assertEquals(wrapper.getMarketOrderListByPair("_", "1000148").getOrders(0).getSellTokenId().toStringUtf8(), "_");
        //query non-existent token-id for empty result
        Assert.assertTrue(wrapper.getMarketOrderListByPair("1005554", "1005555").getOrdersList().isEmpty());
        Assert.assertTrue(wrapper.getMarketOrderListByPair("1000140", "1005555").getOrdersList().isEmpty());
        Assert.assertTrue(wrapper.getMarketOrderListByPair("1005560", "1000150").getOrdersList().isEmpty());
        //query invalid param
        try {
            logger.info(wrapper.getMarketOrderListByPair("TX74o6dWugAgdaMv8M39QP9YL5QRgfj32t", "1672128873000").toString());
            Assert.fail();
        } catch (StatusRuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "INTERNAL: sellTokenId is not a valid number");
        }
        try {
            logger.info(wrapper.getMarketOrderListByPair("1000323", "TX74o6dWugAgdaMv8M39QP9YL5QRgfj32t").toString());
            Assert.fail();
        } catch (StatusRuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "INTERNAL: buyTokenId is not a valid number");
        }
    }

    @Test(enabled = true)
    public void test04getMarketPriceByPair() throws Exception {
        //get price by pair and make sure price be reduced
        Response.MarketPrice mpp = wrapper.getMarketPriceByPair("1000598", "1000349").getPrices(0);
        Assert.assertEquals(mpp.getSellTokenQuantity(), 3);
        Assert.assertEquals(mpp.getBuyTokenQuantity(), 4);
        //there are more than one price's order
        Assert.assertEquals(wrapper.getMarketPriceByPair("1005057","_").getPricesList().size(),2);
        //query between trc10 token and TRX
        Response.MarketPrice sep1 = wrapper.getMarketPriceByPair("1000323", "_").getPrices(0);
        Assert.assertEquals(sep1.getSellTokenQuantity(), 2);
        Assert.assertEquals(sep1.getBuyTokenQuantity(), 1);
        Response.MarketPrice sep2 = wrapper.getMarketPriceByPair("_", "1000148").getPrices(0);
        Assert.assertEquals(sep2.getSellTokenQuantity(), 2);
        Assert.assertEquals(sep2.getBuyTokenQuantity(), 1);
        //query non-existent token-id for empty result
        Assert.assertTrue(wrapper.getMarketPriceByPair("1005554", "1005555").getPricesList().isEmpty());
        Assert.assertTrue(wrapper.getMarketPriceByPair("1000140", "1005555").getPricesList().isEmpty());
        Assert.assertTrue(wrapper.getMarketPriceByPair("1005560", "1000150").getPricesList().isEmpty());
        //query invalid param
        try {
            logger.info(wrapper.getMarketPriceByPair("TX74o6dWugAgdaMv8M39QP9YL5QRgfj32t", "1000340").toString());
            Assert.fail();
        } catch (StatusRuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "INTERNAL: sellTokenId is not a valid number");
        }
        try {
            logger.info(wrapper.getMarketPriceByPair("1000323", "TX74o6dWugAgdaMv8M39QP9YL5QRgfj32t").toString());
            Assert.fail();
        } catch (StatusRuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "INTERNAL: buyTokenId is not a valid number");
        }
    }

    @Test(enabled = true)
    public void test05getPaginatedExchangeList() throws Exception {
        //random offset and limit
        Random random = new Random();
        int[] limits = {5, 10, 20, 50};
        for (int i = 0; i < 5; i++) {
            int offset = (random.nextInt(10) + 1) * 5;
            int limit = limits[random.nextInt(limits.length)];
            Response.ExchangeList exchangeList = wrapper.getPaginatedExchangeList(offset, limit);
            logger.info(String.valueOf(exchangeList.getExchangesCount()));
            Assert.assertTrue(exchangeList.getExchangesCount() <= limit);
            Assert.assertEquals(exchangeList.getExchanges(0).getExchangeId(), offset + 1);
        }
        //offset >= exchange amount, result is empty
        Response.ExchangeList exchangeList = wrapper.getPaginatedExchangeList(5000, 3);
        Assert.assertEquals(exchangeList.getExchangesCount(), 0);
        //limit >= exchange amount, get all the result
        Response.ExchangeList exchangeList1 = wrapper.getPaginatedExchangeList(0, 5000);
        Assert.assertEquals(exchangeList1.getExchangesCount(), wrapper.listExchanges().getExchangesCount());

    }

    @Test(enabled = true)
    public void test06getMarketPairList() throws Exception {
        int OrderPairCount = wrapper.getMarketPairList().getOrderPairCount();
        Assert.assertNotEquals(OrderPairCount,0);
        Response.MarketOrderPair marketOrderPair = wrapper.getMarketPairList().getOrderPair(new Random().nextInt(OrderPairCount));
        Assert.assertNotEquals(wrapper.getMarketOrderListByPair(marketOrderPair.getSellTokenId().toStringUtf8(),marketOrderPair.getBuyTokenId().toStringUtf8()),null);
    }

    @Test(enabled = true)
    public void test07MarketSellAssetAndCancelOrder() throws Exception {

        //create market order between 2 trc10 tokens
        Response.TransactionExtention transactionSell = wrapper.marketSellAsset(owner,"1005057",20,"1005416",10);
        Chain.Transaction transaction = wrapper.signTransaction(transactionSell);
        String broadcast = wrapper.broadcastTransaction(transaction);

        Assert.assertEquals(wrapper.getMarketOrderByAccount(owner).getOrdersCount(),1);
        String orderId = ByteArray.toHexString(wrapper.getMarketOrderByAccount(owner).getOrders(0).getOrderId().toByteArray());
        logger.info("orderId: {}", orderId);
        Assert.assertEquals(wrapper.getMarketOrderById(orderId).getSellTokenQuantityRemain(),20);

        //cancel the order created just now
        Response.TransactionExtention transactionCancel = wrapper.marketCancelOrder(owner,orderId);
        Chain.Transaction transactionc = wrapper.signTransaction(transactionCancel);
        String broadcastc = wrapper.broadcastTransaction(transactionc);
        logger.info(broadcastc);
        Assert.assertEquals(wrapper.getMarketOrderById(orderId).getState().toString(),"CANCELED");
        Assert.assertEquals(wrapper.getMarketOrderByAccount(owner).getOrdersCount(),0);

        //create market order between trc10 token and TRX，TRX as buyToken
        Response.TransactionExtention transactionSell1 = wrapper.marketSellAsset(owner,"1005057",30,"_",10);
        Chain.Transaction transaction1 = wrapper.signTransaction(transactionSell1);
        String broadcast1 = wrapper.broadcastTransaction(transaction1);

        Assert.assertEquals(wrapper.getMarketOrderByAccount(owner).getOrdersCount(),1);
        String orderId1 = ByteArray.toHexString(wrapper.getMarketOrderByAccount(owner).getOrders(0).getOrderId().toByteArray());
        logger.info("orderId: {}", orderId1);
        Assert.assertEquals(wrapper.getMarketOrderById(orderId1).getSellTokenQuantityRemain(),30);

        //cancel the order created just now
        Response.TransactionExtention transactionCancel1 = wrapper.marketCancelOrder(owner,orderId1);
        Chain.Transaction transactionc1 = wrapper.signTransaction(transactionCancel1);
        String broadcastc1 = wrapper.broadcastTransaction(transactionc1);
        logger.info(broadcastc1);
        Assert.assertEquals(wrapper.getMarketOrderById(orderId1).getState().toString(),"CANCELED");
        Assert.assertEquals(wrapper.getMarketOrderByAccount(owner).getOrdersCount(),0);

        //create market order between trc10 token and TRX，TRX as sellToken
        Response.TransactionExtention transactionSell2 = wrapper.marketSellAsset(owner,"_",30,"1005416",10);
        Chain.Transaction transaction2 = wrapper.signTransaction(transactionSell2);
        String broadcast2 = wrapper.broadcastTransaction(transaction2);

        Assert.assertEquals(wrapper.getMarketOrderByAccount(owner).getOrdersCount(),1);
        String orderId2 = ByteArray.toHexString(wrapper.getMarketOrderByAccount(owner).getOrders(0).getOrderId().toByteArray());
        logger.info("orderId: {}", orderId2);
        Assert.assertEquals(wrapper.getMarketOrderById(orderId2).getSellTokenQuantityRemain(),30);

        //cancel the order created just now
        Response.TransactionExtention transactionCancel2 = wrapper.marketCancelOrder(owner,orderId2);
        Chain.Transaction transactionc2 = wrapper.signTransaction(transactionCancel2);
        String broadcastc2 = wrapper.broadcastTransaction(transactionc2);
        logger.info(broadcastc2);
        Assert.assertEquals(wrapper.getMarketOrderById(orderId2).getState().toString(),"CANCELED");
        Assert.assertEquals(wrapper.getMarketOrderByAccount(owner).getOrdersCount(),0);

    }

    @Test(enabled = true)
    public void test08MarketSellAssetFail() throws Exception {

        //selltokenid not exist
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketSellAsset(owner,"1005678",20,"1005416",10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : No sellTokenId !");
        }
        //selltokequantity <= 0
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketSellAsset(owner,"1005057",-1,"1005416",10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token quantity must greater than zero");
        }
        //address selltoken balance <= selltokequantity
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketSellAsset(owner,"1000416",500000000,"1005057",10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : SellToken balance is not enough !");
        }

        //buytokequantity <= 0
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketSellAsset(owner,"1005057",10,"1000416",0);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast4 = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token quantity must greater than zero");
        }
        //SIGERROR
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketSellAsset("TAvMRE6SL8aYYyVx5mHkEwBPvgD3FGSQND","1005057",20,"1005416",10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertTrue(e.getMessage().startsWith("SIGERROR"));
        }
    }
    @Test(enabled = true)
    public void test09MarketCancelOrderFail() throws Exception {
        //param address is not belong to sell address
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketCancelOrder(owner,"e163dec2e6d4561f20c8b0ab8e9fe0e9309fc2a05ed9c8e843a6fc5c92021e7c");
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : Order does not belong to the account!");
        }

        //orderid not exist
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketCancelOrder(owner,"c67b52feff63a4c600622f194f8c1755c42");
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : orderId not exists");
        }

        //cancel a canceled order
        try{
            Response.TransactionExtention transactionExtention = wrapper.marketCancelOrder(owner,"c67b52feff63a4c600622f194f8c1755c421ac01192ce62adc95df87a954b329");
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : Order is not active!");
        }
    }
}
