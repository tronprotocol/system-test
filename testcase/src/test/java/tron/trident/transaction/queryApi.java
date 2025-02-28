package tron.trident.transaction;

import io.grpc.ClientInterceptor;
import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.proto.Response;
import tron.trident.utils.CustomClientInterceptor;
import tron.trident.utils.TestBase;

public class queryApi extends TestBase  {

  @Test(enabled = true)
  public void test01GetAccount() throws Exception {

    System.out.println(wrapper.getAccount(owner));
  }


  @Test(enabled = true)
  public void test02GetTransactionById() throws Exception {

    System.out.println(wrapper.getTransactionById("11b1a066d16139d1ceecd22eee78b137e167b0a2f4cce7ed3b37c2581e7fed3d"));
  }


  @Test(enabled = true)
  public void test02GetTransactionInfoById() throws Exception {

    System.out.println(wrapper.getTransactionInfoById("c4b649ef6a793e34aa7440902047d197a2cadcac990bf38cdfb51953f7e93969"));
  }


  @Test(enabled = true)
  public void test03ClientInterceptor() throws Exception {
    System.out.println("--------------------------------------------");
    List<ClientInterceptor> clientInterceptors = new ArrayList<>();
    clientInterceptors.add(new CustomClientInterceptor());

    wrapper = new ApiWrapper("grpc.nile.trongrid.io:50051",
        "grpc.nile.trongrid.io:50061", ownerKey,clientInterceptors);

    System.out.println(wrapper.getTransactionInfoById("c4b649ef6a793e34aa7440902047d197a2cadcac990bf38cdfb51953f7e93969"));
  }


  @Test(enabled = true, description = "assert getBlock(true) == getNowBlock2")
  public void test04GetBlockTrueIsSameAsGetNowBlock2(){
    String fullnode = "grpc.nile.trongrid.io:50051";

    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    GrpcAPI.BlockExtention blockExt =  blockingStubFull.getNowBlock2(GrpcAPI.EmptyMessage.newBuilder().build());
    Response.BlockExtention blockExtTrident = wrapper.getBlock(true);
    System.out.println(blockExt);
    System.out.println(blockExtTrident);
    Assert.assertEquals(blockExt.toString(), blockExtTrident.toString());
  }

}
