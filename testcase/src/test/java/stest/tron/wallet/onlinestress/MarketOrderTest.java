package stest.tron.wallet.onlinestress;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.MarketOrder;
import org.tron.protos.Protocol.MarketOrderList;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
public class MarketOrderTest {
  String[] addressList = {
      "3d16a04b7d01712667b40cb7f4c9bbe73ca111ac14a13a4f8e12a7f557d2c89c",
      "978c0a1832fd091c60dff74f9bca04dca39ec118480880f2a7ed2c90b5908640",
      "7f1769c7882c3e9e488251005422f6a5fb373630e84589cff4d95e8b3f8a308f",
      "ce6544fcfe704f74d11dfff1a61df3cc76f2261c5b7b9e3e838f4c2a77643554",
      "8dce7a3612666fc594f6aa3ff29f3eb45902fa0c1a7e3922e21cf7a1409f13cd",
      "b240970dcad14b3a9883359194979d4fdc0a49cbef40bb167d3d1700871ab83e",
      "24c730b524312a43e3b6fdfffb115a85f05e823aaeb375b92751e25abcd95719",
      "d3ace279734e51edd8e74c906bc6f5acdd8a22f0bf5759137d9619b4679effe4",
      "f9f56379ab2be66e3e340621e3cc21514acec1fc7bb72eda3681ab7dc96af809",
      "31a67a8041ff89475466662b3e0bc7164519b1abacdeceac28869b2c15a2330e",
      "c74fb4d8101572041c6fab30e1602ba1ec8247e1ead19641fb985b3ed3a8261e",
      "25f98ac22c9fd02aa8a2ef354db0aa13ebc2a6c31377ea7e2b342f0d3898af0d",
      "939a2cec3768bd2d2834126c20d2b1c513e3711f085ce374f654a7b144aa409f",
      "39862f4dd51972ca22ce50b7b9e629043387000120c33bf263399ad9b334da1a",
      "79045aab0f3199ac456ce2039e809e6c942983ede0e3a398d571dedddb351348",
      "d50fe9c48e95289cde324ffeff095f8275f9ab07375e5e843167a0a54d3e1462",
      "61651f2b8a87e1ae0ced5e700807f2abb50e97fe7d3d3e6a8aa58f0a6b0149a6",
      "bb03d70e5187258ffb6cddb1becade5c1b2606b7ea84636b7dfaeef6216610a5",
      "25858c236634e353d018f310f61e077b78e1410766565ed56ff11ee7410dcf20",
      "ede941a01eb8234866f60c7e8e95db4614bb0d05298d82bae0abea81f1861046",
      "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0"
  };
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =     "39.107.225.170:50051";
  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }
  @Test
  public void markerOrderCancle(){

    while (true) {

      for(String key : addressList){
        byte[] addressByteArray = PublicMethed.getFinalAddress(key);
        List<MarketOrder> OrderList = PublicMethed
            .getMarketOrderByAccount(addressByteArray, blockingStubFull).get().getOrdersList();
        int single_times = 0;
        for(MarketOrder order : OrderList){
          byte[] orderId = order.getOrderId().toByteArray();
          String txid = PublicMethed
              .marketCancelOrder(addressByteArray, key, orderId, blockingStubFull);
          if(single_times++ >= 2) {
            break;
          }
        }
      }
    }
  }
}