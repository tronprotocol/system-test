package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.ProposalEnum;

public class HttpTestResourcePrices {
  private JSONObject responseContent;
  private HttpResponse response;
  private String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(0);
  private String httpSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(2);
  private String httpPbftNode = Configuration.getByPath("testng.conf")
      .getStringList("httpnode.ip.list").get(4);


  @Test(enabled = true, description = "get MemoFee from http interface")
  public void test01GetMemoFee() {
    //get MemoFee from chainParameters
    Long memoFeeChainParameter = HttpMethed.getProposalValue(httpnode, ProposalEnum.GetMemoFee.getProposalName());

    //get MemoFee from http
    HttpResponse res =  HttpMethed.getMemoFee(httpnode);
    String prices =  HttpMethed.parseResponseContent(res).getString("prices");
    Long memoNow = Long.parseLong(prices.split(":")[1]);

    Assert.assertEquals(memoFeeChainParameter.longValue(), memoNow.longValue());
  }

  @Test(enabled = true, description = "get Energy prices from http interface")
  public void test02GetEnergyPrices() {
    //get EnergyPrice from chainParameters
    Long energyPriceChainParameter = HttpMethed.getProposalValue(httpnode, ProposalEnum.GetEnergyFee.getProposalName());

    //get EnergyPrice from http
    HttpResponse res = HttpMethed.getEnergyPric(httpnode);
    String prices = HttpMethed.parseResponseContent(res).getString("prices");
    Long energyPriceNow = Long.parseLong(prices.split(":")[1]);

    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPriceNow.longValue());

    //get EnergyPrice from http solidity
    HttpResponse resSolidity = HttpMethed.getEnergyPricSolidity(httpSoliditynode);
    String pricesSolidity = HttpMethed.parseResponseContent(resSolidity).getString("prices");
    Long energyPriceSolidity = Long.parseLong(pricesSolidity.split(":")[1]);
    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPriceSolidity.longValue());

    //get EnergyPrice from http pbft
    HttpResponse resPbft = HttpMethed.getEnergyPricPbft(httpPbftNode);
    String pricesPbft = HttpMethed.parseResponseContent(resPbft).getString("prices");
    Long energyPricePbft = Long.parseLong(pricesPbft.split(":")[1]);
    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPricePbft.longValue());
  }

  @Test(enabled = true, description = "get Bandwidth prices from http interface")
  public void test02GetBandwidthPrices() {
    //get Bandwidth prices from chainParameters
    Long BandwidthPriceChainParameter = HttpMethed.getProposalValue(httpnode, ProposalEnum.getTransactionFee.getProposalName());

    //get BandwidthPrice from http
    HttpResponse res = HttpMethed.getBandPric(httpnode);
    String prices = HttpMethed.parseResponseContent(res).getString("prices");
    Long BandwidthPriceNow = Long.parseLong(prices.split(":")[1]);

    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), BandwidthPriceNow.longValue());

    //get BandwidthPrice from http solidity
    HttpResponse resSolidity = HttpMethed.getBandPricSolidity(httpSoliditynode);
    String pricesSolidity = HttpMethed.parseResponseContent(resSolidity).getString("prices");
    Long bandwidthPriceSolidity = Long.parseLong(pricesSolidity.split(":")[1]);
    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), bandwidthPriceSolidity.longValue());

    //get BandwidthPrice from http pbft
    HttpResponse resPbft = HttpMethed.getBandPricPbft(httpPbftNode);
    String pricesPbft = HttpMethed.parseResponseContent(resPbft).getString("prices");
    Long bandwidthPricePbft = Long.parseLong(pricesPbft.split(":")[1]);
    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), bandwidthPricePbft.longValue());
  }
}
