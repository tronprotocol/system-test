package stest.tron.wallet.dailybuild.http;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethod;
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
  private String httpnodeSolidityPort = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
      .get(3);


  @Test(enabled = true, description = "get MemoFee from http interface", groups = {"daily", "serial"})
  public void test01GetMemoFee() {
    //get MemoFee from chainParameters
    Long memoFeeChainParameter = HttpMethod.getProposalValue(httpnode, ProposalEnum.GetMemoFee.getProposalName());

    //get MemoFee from http
    HttpResponse res =  HttpMethod.getMemoFee(httpnode);
    String prices =  HttpMethod.parseResponseContent(res).getString("prices");
    Long memoNow = Long.parseLong(prices.split(":")[2]);

    Assert.assertEquals(memoFeeChainParameter.longValue(), memoNow.longValue());
  }

  @Test(enabled = true, description = "get Energy prices from http interface", groups = {"daily", "serial"})
  public void test02GetEnergyPrices() {
    //get EnergyPrice from chainParameters
    Long energyPriceChainParameter = HttpMethod.getProposalValue(httpnode, ProposalEnum.GetEnergyFee.getProposalName());

    //get EnergyPrice from http
    HttpResponse res = HttpMethod.getEnergyPric(httpnode);
    String prices = HttpMethod.parseResponseContent(res).getString("prices");
    Long energyPriceNow = Long.parseLong(prices.split(":")[2]);

    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPriceNow.longValue());

    //get EnergyPrice from http solidityNode
    HttpResponse resSolidity = HttpMethod.getEnergyPricSolidity(httpSoliditynode);
    String pricesSolidity = HttpMethod.parseResponseContent(resSolidity).getString("prices");
    Long energyPriceSolidity = Long.parseLong(pricesSolidity.split(":")[2]);
    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPriceSolidity.longValue());

    //get EnergyPrice from FullNode Solidity port
    HttpResponse resSolidityport = HttpMethod.getEnergyPricSolidity(httpnodeSolidityPort);
    String pricesSolidityPort = HttpMethod.parseResponseContent(resSolidityport).getString("prices");
    Long energyPriceSolidityPort = Long.parseLong(pricesSolidityPort.split(":")[2]);
    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPriceSolidityPort.longValue());

    //get EnergyPrice from http pbft
    HttpResponse resPbft = HttpMethod.getEnergyPricPbft(httpPbftNode);
    String pricesPbft = HttpMethod.parseResponseContent(resPbft).getString("prices");
    Long energyPricePbft = Long.parseLong(pricesPbft.split(":")[2]);
    Assert.assertEquals(energyPriceChainParameter.longValue(), energyPricePbft.longValue());
  }

  @Test(enabled = true, description = "get Bandwidth prices from http interface", groups = {"daily", "serial"})
  public void test02GetBandwidthPrices() {
    //get Bandwidth prices from chainParameters
    Long BandwidthPriceChainParameter = HttpMethod.getProposalValue(httpnode, ProposalEnum.getTransactionFee.getProposalName());

    //get BandwidthPrice from http
    HttpResponse res = HttpMethod.getBandPric(httpnode);
    String prices = HttpMethod.parseResponseContent(res).getString("prices");
    Long BandwidthPriceNow = Long.parseLong(prices.split(":")[2]);

    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), BandwidthPriceNow.longValue());

    //get BandwidthPrice from http solidity
    HttpResponse resSolidity = HttpMethod.getBandPricSolidity(httpSoliditynode);
    String pricesSolidity = HttpMethod.parseResponseContent(resSolidity).getString("prices");
    Long bandwidthPriceSolidity = Long.parseLong(pricesSolidity.split(":")[2]);
    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), bandwidthPriceSolidity.longValue());

    //get BandwidthPrice from FullNode solidity port
    HttpResponse resSolidityPort = HttpMethod.getBandPricSolidity(httpnodeSolidityPort);
    String pricesSolidityPort = HttpMethod.parseResponseContent(resSolidityPort).getString("prices");
    Long bandwidthPriceSolidityPort = Long.parseLong(pricesSolidityPort.split(":")[2]);
    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), bandwidthPriceSolidityPort.longValue());

    //get BandwidthPrice from http pbft
    HttpResponse resPbft = HttpMethod.getBandPricPbft(httpPbftNode);
    String pricesPbft = HttpMethod.parseResponseContent(resPbft).getString("prices");
    Long bandwidthPricePbft = Long.parseLong(pricesPbft.split(":")[2]);
    Assert.assertEquals(BandwidthPriceChainParameter.longValue(), bandwidthPricePbft.longValue());
  }
}
