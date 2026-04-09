package tron.trident.client;

import io.grpc.ClientInterceptor;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.Constant;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.proto.Chain;
import tron.trident.utils.TestBase;

import java.util.ArrayList;
import java.util.List;



@Slf4j
public class TestApiKey extends TestBase {

  @Test(enabled = true, description = "test header with api key")
  public void test01HeaderWithApiKey() throws IllegalException {
    wrapper.close();
    wrapper = new ApiWrapper(
        Constant.FULLNODE_NILE,
        Constant.FULLNODE_NILE_SOLIDITY,
        ownerKey, "dba1984e-b96d-4a6c-ac8f-919409625ba9"
    );
    Chain.Block block = wrapper.getNowBlock();
    logger.info(block.toString());
    Assert.assertTrue(block.toString().contains("block_header"));
    wrapper.close();
  }

  @Test(enabled = true, description = "test header with clientInterceptors")
  public void test02HeaderWithClientInterceptor() throws IllegalException {
    wrapper.close();
    List<ClientInterceptor> clientInterceptors = new ArrayList<>();
    //custom header Class
    ClientInterceptor interceptors = new HeaderClientInterceptor();
    clientInterceptors.add(interceptors);
    wrapper = new ApiWrapper(
        Constant.FULLNODE_NILE,
        Constant.FULLNODE_NILE_SOLIDITY,
        ownerKey, clientInterceptors
    );
    Chain.Block block = wrapper.getNowBlock();
    logger.info(block.toString());
    Assert.assertTrue(block.toString().contains("block_header"));
    wrapper.close();
  }





}
