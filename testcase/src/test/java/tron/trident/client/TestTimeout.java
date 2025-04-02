package tron.trident.client;

import io.grpc.ClientInterceptor;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.Constant;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TestTimeout extends TestBase {

  @Test(enabled = true, description = "test timeout 10ms request failed")
  public void test01TimeOutFailed() throws IllegalException {
    wrapper.close();
    List<ClientInterceptor> clientInterceptors = new ArrayList<>();
    wrapper = new ApiWrapper(
        Constant.FULLNODE_NILE,
        Constant.FULLNODE_NILE_SOLIDITY,
        ownerKey, clientInterceptors, 10
    );
    try {
      Chain.Block block = wrapper.getNowBlock();
    } catch (StatusRuntimeException e) {
      logger.info("assert timeout exception: ");
      logger.info(e.toString());
      Assert.assertTrue(e.toString().contains("DEADLINE_EXCEEDED"));
      return;
    } finally {
      wrapper.close();
    }
    // if not,  case failed
    Assert.fail();
  }


  @Test(enabled = true, description = "test timeout 10ms request failed")
  public void test02TimeOutFailedSolidity() throws IllegalException {
    wrapper.close();
    wrapper = new ApiWrapper(
        Constant.FULLNODE_NILE,
        Constant.FULLNODE_NILE_SOLIDITY,
        ownerKey, 10
    );
    try {
      Response.Account account = wrapper.getAccountSolidity(owner);
    } catch (StatusRuntimeException e) {
      logger.info("assert timeout exception: ");
      logger.info(e.toString());
      Assert.assertTrue(e.toString().contains("DEADLINE_EXCEEDED"));
      return;
    } finally {
      wrapper.close();
    }
    // if not,  case failed
    Assert.fail();
  }

  @Test(enabled = true, description = "test timeout 2000ms request success")
  public void test03TimeOutFailed() throws IllegalException {
    wrapper.close();
    List<ClientInterceptor> clientInterceptors = new ArrayList<>();
    wrapper = new ApiWrapper(
        Constant.FULLNODE_NILE,
        Constant.FULLNODE_NILE_SOLIDITY,
        ownerKey, clientInterceptors, 2000
    );
    try {
      Chain.Block block = wrapper.getNowBlock();
      logger.info(block.toString());
      Assert.assertTrue(block.toString().contains("number"));
    } catch (Exception e) {
      logger.info(e.toString());
      Assert.fail();
    } finally {
      wrapper.close();
    }
  }



}
