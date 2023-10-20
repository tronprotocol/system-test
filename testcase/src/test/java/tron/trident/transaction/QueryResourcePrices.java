package tron.trident.transaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.trident.abi.TypeDecoder;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;

public class QueryResourcePrices extends TestBase {
  @Test(enabled = true)
  public void test01GetBandwidthPrices() throws Exception {
    String res = wrapper.getBandwidthPrices().toString();
    System.out.println(res);
    Assert.assertTrue(res.contains(":"));

    String resSolidity = wrapper.getBandwidthPricesOnSolidity().toString();
    System.out.println(resSolidity);
    Assert.assertTrue(resSolidity.contains(":"));
  }

  @Test(enabled = true)
  public void test01GetEnergyPrices() throws Exception {
    String res = wrapper.getEnergyPrices().toString();
    System.out.println(res);
    Assert.assertTrue(res.contains(":"));

    String resSolidity = wrapper.getEnergyPricesOnSolidity().toString();
    System.out.println(resSolidity);
    Assert.assertTrue(resSolidity.contains(":"));

  }

  @Test(enabled = true)
  public void test01GetMemoFee() throws Exception {
    String res = wrapper.getMemoFee().toString();
    System.out.println(res);
    Assert.assertTrue(res.contains(":"));
  }
}
