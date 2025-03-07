package tron.trident.market;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import stest.tron.wallet.common.client.utils.ByteArray;
import tron.trident.utils.TestBase;
import static org.tron.trident.core.Constant.*;


@Slf4j
public class exchangeTransaction extends TestBase {

    @Test(enabled = false)
    public void test01exchangeCreate() throws Exception {

        //create exchange between 2 trc10 tokens
        Response.TransactionExtention transactionExtention1 = wrapper.exchangeCreate(owner,"1005057",20,"1005416",8);
        Chain.Transaction transaction1 = wrapper.signTransaction(transactionExtention1);
        String broadcast1 = wrapper.broadcastTransaction(transaction1);
        logger.info("create exchange success, txid is :" + broadcast1);//0761d768a78052788776e85f7353c6378109f05515964c1e25b1d94824bcf955

        //create exchange between trc10 token and TRX, TRX as secondtoken
        Response.TransactionExtention transactionExtention2 = wrapper.exchangeCreate(owner,"1005057",20,"_",3000);
        Chain.Transaction transaction2 = wrapper.signTransaction(transactionExtention2);
        String broadcast2 = wrapper.broadcastTransaction(transaction2);
        logger.info("create exchange success, txid is :" + broadcast2);//07b47fa1c496cf84c240e42d30f0965c8644631a589a24c3ac30010207d0aa93

        //create exchange between trc10 token and TRX, TRX as firsttoken
        Response.TransactionExtention transactionExtention3 = wrapper.exchangeCreate(owner,"_",5000,"1005416",12);
        Chain.Transaction transaction3 = wrapper.signTransaction(transactionExtention3);
        String broadcast3 = wrapper.broadcastTransaction(transaction3);
        logger.info("create exchange success, txid is :" + broadcast3);//b5e9982914e544f909dc7f79011186273a978403b2f8f0e50404f365a00110eb


        //re-create exchange of the same tokens have created before
        Response.TransactionExtention transactionExtention4 = wrapper.exchangeCreate(owner,"1005057",30,"_",5000);
        Chain.Transaction transaction4 = wrapper.signTransaction(transactionExtention4);
        String broadcast4 = wrapper.broadcastTransaction(transaction4);
        logger.info("create exchange success, txid is :" + broadcast4);//961d2b2039a616737fb1711feb323409a39db4abdcbf663b56f39f690847d659

        int exchangeCount = wrapper.listExchanges().getExchangesCount();
        Response.ExchangeList exchangeList =  wrapper.listExchanges();
        for (Response.Exchange exchange : exchangeList.getExchangesList()) {
            if (exchange.getExchangeId() == exchangeCount - 3) {
                Assert.assertEquals(exchange.getFirstTokenId().toStringUtf8(),"1005057");
                Assert.assertEquals(exchange.getSecondTokenId().toStringUtf8(),"1005416");
            }
            if (exchange.getExchangeId() == exchangeCount - 2) {
                Assert.assertEquals(exchange.getFirstTokenId().toStringUtf8(),"1005057");
                Assert.assertEquals(exchange.getSecondTokenId().toStringUtf8(),"_");
            }
            if (exchange.getExchangeId() == exchangeCount - 1) {
                Assert.assertEquals(exchange.getFirstTokenId().toStringUtf8(),"_");
                Assert.assertEquals(exchange.getSecondTokenId().toStringUtf8(),"1005416");
            }

        }

    }
    @Test(enabled = true)
    public void test02exchangeCreate() throws Exception {
        //error: token1 = token2
        try {
            Response.TransactionExtention transactionExtention = wrapper.exchangeCreate(owner,"_",20,"_",8);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : cannot exchange same tokens");
        }
    }

    @Test(enabled = true)
    public void test03exchangeCreateFail() throws Exception {

        //fail: first/second-balance <= 0
        try {
            Response.TransactionExtention transactionExtention = wrapper.exchangeCreate(owner,"1005057",10000000,"1005416",0);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token balance must greater than zero");
        }

        try {
            Response.TransactionExtention transactionExtention = wrapper.exchangeCreate(owner,"1005057",-1,"1005416",1000);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token balance must greater than zero");
        }

    }

    @Test(enabled = true)
    public void test04exchangeCreateFail() throws Exception {

        //address firsttoken balance not enough
        try {
            Response.TransactionExtention transactionExtention = wrapper.exchangeCreate(owner,"1000100",1000,"1005416",1000);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : first token balance is not enough");
        }
        //address secondtoken balance not enough
        try {
            Response.TransactionExtention transactionExtention = wrapper.exchangeCreate(owner,"1005057",5,"1000100",1000);
            Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : second token balance is not enough");
        }
        //a new address has not enough TRX as create fee
        try {
            ApiWrapper client = new ApiWrapper(FULLNODE_NILE,
                    FULLNODE_NILE_SOLIDITY,"274bfa15aa672cbff3ef462ff9df5733427b85fe6c3223196a770c0a47e02f01");
            Response.TransactionExtention transactionExtention = client.exchangeCreate("TRXohW1cUgfSg5d6SkDRNK2JEXjBdhkspH","1005416",1,"1005057",1);
            Chain.Transaction transaction = client.signTransaction(transactionExtention);
            String broadcast = client.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : No enough balance for exchange create fee!");
        }
    }


    @Test(enabled = true)
    public void test05exchangeInjectAndWithdraw() throws Exception {

        //exchange Inject between 2 trc10 tokens, exchange balance changed
        Response.TransactionExtention transactionInject = wrapper.exchangeInject(owner,64,"1005057",5);
        Chain.Transaction transaction = wrapper.signTransaction(transactionInject);
        String broadcast = wrapper.broadcastTransaction(transaction);
        //TRX as second token and inject first token
        Response.TransactionExtention transactionInject1 = wrapper.exchangeInject(owner,65,"1005057",10);
        Chain.Transaction transaction1 = wrapper.signTransaction(transactionInject1);
        String broadcast1 = wrapper.broadcastTransaction(transaction1);
        //TRX as first token and inject second token
        Response.TransactionExtention transactionInject2 = wrapper.exchangeInject(owner,66,"1005416",3);
        Chain.Transaction transaction2 = wrapper.signTransaction(transactionInject2);
        String broadcast2 = wrapper.broadcastTransaction(transaction2);

        Response.ExchangeList exchangeList =  wrapper.listExchanges();
        for (Response.Exchange exchange : exchangeList.getExchangesList()) {
            if (exchange.getExchangeId() == 64) {
                Assert.assertEquals(exchange.getSecondTokenBalance(),10);
            }
            if (exchange.getExchangeId() == 65) {
                Assert.assertEquals(exchange.getSecondTokenBalance(),4500);
            }
            if (exchange.getExchangeId() == 66) {
                Assert.assertEquals(exchange.getFirstTokenBalance(),6250);
            }
        }

        //exchange withdraw between 2 trc10 tokens, exchange balance changed back
        Response.TransactionExtention transactionWithdraw = wrapper.exchangeWithdraw(owner,64,"1005057",5);
        Chain.Transaction transactionw = wrapper.signTransaction(transactionWithdraw);
        String broadcastw = wrapper.broadcastTransaction(transactionw);
        //withdraw and change back
        Response.TransactionExtention transactionWithdraw1 = wrapper.exchangeWithdraw(owner,65,"_",1500);
        Chain.Transaction transactionw1 = wrapper.signTransaction(transactionWithdraw1);
        String broadcastw1 = wrapper.broadcastTransaction(transactionw1);
        //withdraw and change back
        Response.TransactionExtention transactionWithdraw2 = wrapper.exchangeWithdraw(owner,66,"1005416",3);
        Chain.Transaction transactionw2 = wrapper.signTransaction(transactionWithdraw2);
        String broadcastw2 = wrapper.broadcastTransaction(transactionw2);

        Response.ExchangeList exchangeListw =  wrapper.listExchanges();
        for (Response.Exchange exchange : exchangeListw.getExchangesList()) {
            if (exchange.getExchangeId() == 64) {
                Assert.assertEquals(exchange.getSecondTokenBalance(),8);
            }
            if (exchange.getExchangeId() == 65) {
                Assert.assertEquals(exchange.getFirstTokenBalance(),20);
            }
            if (exchange.getExchangeId() == 66) {
                Assert.assertEquals(exchange.getFirstTokenBalance(),5000);
            }
        }

    }

    @Test(enabled = true)
    public void test06exchangeWithdrawAll() throws Exception {

        //withdraw all the balance of last exchange
//        int exchangeid = wrapper.listExchanges().getExchangesCount();
//        Response.TransactionExtention transactionWithdraw = wrapper.exchangeWithdraw(owner,exchangeid,"1005057",30);
//        Chain.Transaction transactionw = wrapper.signTransaction(transactionWithdraw);
//        String broadcastw = wrapper.broadcastTransaction(transactionw);
//        Response.ExchangeList exchangeListw =  wrapper.listExchanges();
//        for (Response.Exchange exchange : exchangeListw.getExchangesList()) {
//            if (exchange.getExchangeId() == 67) {
//                Assert.assertEquals(exchange.getSecondTokenBalance(),0);
//            }
//        }
        //inject for empty exchange
        try{
            Response.TransactionExtention transactionInject = wrapper.exchangeInject(owner,67,"1005057",30);
            Chain.Transaction transaction = wrapper.signTransaction(transactionInject);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertTrue(e.getMessage().equals("CONTRACT_VALIDATE_ERROR, Contract validate error : Token balance in exchange is equal with 0,the exchange has been closed"));
        }
        //withdraw a empty exchange
        try{
            Response.TransactionExtention transactionInject = wrapper.exchangeWithdraw(owner,67,"1005057",30);
            Chain.Transaction transaction = wrapper.signTransaction(transactionInject);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertTrue(e.getMessage().equals("CONTRACT_VALIDATE_ERROR, Contract validate error : Token balance in exchange is equal with 0,the exchange has been closed"));
        }

    }


    @Test(enabled = true)
    public void test07exchangeInjectFail() throws Exception {

        //the injected address is not exchange creater
        try{
            ApiWrapper client = new ApiWrapper(FULLNODE_NILE,
                    FULLNODE_NILE_SOLIDITY,"274bfa15aa672cbff3ef462ff9df5733427b85fe6c3223196a770c0a47e02f01");
            Response.TransactionExtention transactionExtention = client.exchangeInject("TRXohW1cUgfSg5d6SkDRNK2JEXjBdhkspH",57,"1005057",10);
            Chain.Transaction transaction = client.signTransaction(transactionExtention);
            String broadcast = client.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : account[41aab4294ef7873b8642d24c04f2523fcfccb24727] is not creator");
        }
        //exchange id is not exist
        try{
            Response.TransactionExtention transactionExtention1 = wrapper.exchangeInject(owner,1000,"1005057",10);
            Chain.Transaction transaction1 = wrapper.signTransaction(transactionExtention1);
            String broadcast1 = wrapper.broadcastTransaction(transaction1);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : Exchange[1000] not exists");
        }
        //inject token id is not exist
        try{
            Response.TransactionExtention transactionExtention2 = wrapper.exchangeInject(owner,67,"—",10);
            Chain.Transaction transaction2 = wrapper.signTransaction(transactionExtention2);
            String broadcast2 = wrapper.broadcastTransaction(transaction2);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token id is not a valid number");
        }
        //address has not enough injected token balance
        try{
            Response.TransactionExtention transactionExtention3 = wrapper.exchangeInject(owner,64,"1005416",20000000000L);
            Chain.Transaction transaction3 = wrapper.signTransaction(transactionExtention3);
            String broadcast3 = wrapper.broadcastTransaction(transaction3);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token balance is not enough");
        }
        //address has not enough the calculated another injected token balance
        try{
            Response.TransactionExtention transactionExtention4 = wrapper.exchangeInject(owner,64,"1005057",2500000000L);
            Chain.Transaction transaction4 = wrapper.signTransaction(transactionExtention4);
            String broadcast4 = wrapper.broadcastTransaction(transaction4);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : another token balance is not enough");
        }
        //inject amount <= 0
        try{
            Response.TransactionExtention transactionExtention5 = wrapper.exchangeInject(owner,64,"1005416",-1);
            Chain.Transaction transaction5 = wrapper.signTransaction(transactionExtention5);
            String broadcast5 = wrapper.broadcastTransaction(transaction5);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : injected token quant must greater than zero");
        }
        //token id is not in exchange
        try{
            Response.TransactionExtention transactionExtention5 = wrapper.exchangeInject(owner,64,"_",-1);
            Chain.Transaction transaction5 = wrapper.signTransaction(transactionExtention5);
            String broadcast5 = wrapper.broadcastTransaction(transaction5);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token id is not in exchange");
        }
    }


    @Test(enabled = true)
    public void test08exchangeWithdrawFail() throws Exception {
        //the withdrew address is not exchange creater
        try{
            ApiWrapper client = new ApiWrapper(FULLNODE_NILE,
                FULLNODE_NILE_SOLIDITY,"274bfa15aa672cbff3ef462ff9df5733427b85fe6c3223196a770c0a47e02f01");
            Response.TransactionExtention transactionExtention = client.exchangeWithdraw("TRXohW1cUgfSg5d6SkDRNK2JEXjBdhkspH",57,"1005057",10);
            Chain.Transaction transaction = client.signTransaction(transactionExtention);
            String broadcast = client.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : account[41aab4294ef7873b8642d24c04f2523fcfccb24727] is not creator");
        }
        //exchange id is not exist
        try{
            Response.TransactionExtention transactionExtention1 = wrapper.exchangeWithdraw(owner,1000,"1005057",10);
            Chain.Transaction transaction1 = wrapper.signTransaction(transactionExtention1);
            String broadcast1 = wrapper.broadcastTransaction(transaction1);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : Exchange[1000] not exists");
        }
        //withdrew token id is not exist
        try{
            Response.TransactionExtention transactionExtention2 = wrapper.exchangeWithdraw(owner,64,"—",10);
            Chain.Transaction transaction2 = wrapper.signTransaction(transactionExtention2);
            String broadcast2 = wrapper.broadcastTransaction(transaction2);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token id is not a valid number");
        }
        //withdrew amount > the exchange balance
        try{
            Response.TransactionExtention transactionExtention3 = wrapper.exchangeWithdraw(owner,64,"1005416",50);
            Chain.Transaction transaction3 = wrapper.signTransaction(transactionExtention3);
            String broadcast3 = wrapper.broadcastTransaction(transaction3);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : exchange balance is not enough");
        }
        //token is not in exchange
        try{
            Response.TransactionExtention transactionExtention5 = wrapper.exchangeWithdraw(owner,64,"1001000",-1);
            Chain.Transaction transaction5 = wrapper.signTransaction(transactionExtention5);
            String broadcast5 = wrapper.broadcastTransaction(transaction5);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token is not in exchange");
        }
        //withdrew amount <= 0
        try{
            Response.TransactionExtention transactionExtention5 = wrapper.exchangeWithdraw(owner,64,"1005416",-1);
            Chain.Transaction transaction5 = wrapper.signTransaction(transactionExtention5);
            String broadcast5 = wrapper.broadcastTransaction(transaction5);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : withdraw token quant must greater than zero");
        }
    }

    @Test(enabled = true)
    public void test09exchange() throws Exception {

        //account sell second token to get first token,exchange first-- second++
        long ftb00 = wrapper.getExchangeById("68").getFirstTokenBalance();
        long stb00 = wrapper.getExchangeById("68").getSecondTokenBalance();
        Response.TransactionExtention transactionExtention = wrapper.exchangeTransaction(owner,68,"_",10,1);
        String txid = ByteArray.toHexString(transactionExtention.getTxid().toByteArray());
        Chain.Transaction transaction = wrapper.signTransaction(transactionExtention);
        String broadcast = wrapper.broadcastTransaction(transaction);
        Thread.sleep(20000);
        logger.info(wrapper.getTransactionInfoById(txid).toString());
        logger.info(String.valueOf(wrapper.getTransactionInfoById(txid).getExchangeReceivedAmount()));
        long exchangeReceivedAmount = wrapper.getTransactionInfoById(txid).getExchangeReceivedAmount();
        long ftb01 = wrapper.getExchangeById("68").getFirstTokenBalance();
        long stb01 = wrapper.getExchangeById("68").getSecondTokenBalance();
        Assert.assertEquals(ftb00-ftb01,exchangeReceivedAmount);
        Assert.assertEquals(stb01-stb00,10);

        //account sell first token to get second token,exchange first++ second--
        long ftb10 = wrapper.getExchangeById("68").getFirstTokenBalance();
        long stb10 = wrapper.getExchangeById("68").getSecondTokenBalance();
        Response.TransactionExtention transactionExtention1 = wrapper.exchangeTransaction(owner,68,"1005057",1,3);
        String txid1 = ByteArray.toHexString(transactionExtention1.getTxid().toByteArray());
        Chain.Transaction transaction1 = wrapper.signTransaction(transactionExtention1);
        String broadcast1 = wrapper.broadcastTransaction(transaction1);
        Thread.sleep(20000);
        logger.info(wrapper.getTransactionInfoById(txid1).toString());
        logger.info(String.valueOf(wrapper.getTransactionInfoById(txid1).getExchangeReceivedAmount()));
        long exchangeReceivedAmount1 = wrapper.getTransactionInfoById(txid1).getExchangeReceivedAmount();
        long ftb11 = wrapper.getExchangeById("68").getFirstTokenBalance();
        long stb11 = wrapper.getExchangeById("68").getSecondTokenBalance();
        Assert.assertEquals(ftb11-ftb10,1);
        Assert.assertEquals(stb10-stb11,exchangeReceivedAmount1);


    }

    @Test(enabled = true)
    public void test10exchangeFail() throws Exception {
        //exchange_id is not exist
        try{
            Response.TransactionExtention transactionFail = wrapper.exchangeTransaction(owner,188,"_",5,10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionFail);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : Exchange[188] not exists");
        }
        //token is not in exchange
        try{
            Response.TransactionExtention transactionFail = wrapper.exchangeTransaction(owner,64,"1000100",5,10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionFail);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token is not in exchange");
        }
        //token id is not a valid number
        try{
            Response.TransactionExtention transactionFail = wrapper.exchangeTransaction(owner,64,"-",5,10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionFail);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token id is not a valid number");
        }
        //except <= 0
        try{
            Response.TransactionExtention transactionFail = wrapper.exchangeTransaction(owner,64,"1005416",12,0);
            Chain.Transaction transaction = wrapper.signTransaction(transactionFail);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token expected must greater than zero");
        }
        //address has not enough token balance
        try{
            Response.TransactionExtention transactionFail = wrapper.exchangeTransaction(owner,64,"1005416",1000000000,10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionFail);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token balance is not enough");
        }
        //exchange cannot afford exchange transaction
        try{
            Response.TransactionExtention transactionFail = wrapper.exchangeTransaction(owner,64,"1005057",12,10);
            Chain.Transaction transaction = wrapper.signTransaction(transactionFail);
            String broadcast = wrapper.broadcastTransaction(transaction);
            Assert.fail();
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
            Assert.assertEquals(e.getMessage(), "CONTRACT_VALIDATE_ERROR, Contract validate error : token required must greater than expected");
        }
    }
}
