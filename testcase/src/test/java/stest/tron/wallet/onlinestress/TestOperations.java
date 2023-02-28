package stest.tron.wallet.onlinestress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.ByteArray;


@Slf4j
public class TestOperations {

  @Test(enabled = true)
  public void test002() {
    Integer[] contractId = {0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 30, 31, 32, 33,
        41, 42, 43, 44, 45, 46, 48, 49, 52, 53, 54, 55, 56, 57, 58};
    List<Integer> list = new ArrayList<>(Arrays.asList(contractId));
    byte[] operations = new byte[32];
    list.forEach(e -> {
      operations[e / 8] |= (1 << e % 8);
    });
    //77ff07c0023e0300000000000000000000000000000000000000000000000000
    logger.info(ByteArray.toHexString(operations));
  }

  @Test(enabled = true)
  public void test003() {
    String operations = "7fff1fc0033ef307000000000000000000000000000000000000000000000000";
    List<Integer> contractId = new ArrayList<>();
    for (int i = 0; i < operations.length(); i = i + 2) {
      int operation16 = Integer.valueOf(operations.substring(i, i + 2), 16);
      for (int n = 0; n < 8; n++) {
        int tmp = 1 << n;
        if ((tmp & operation16) == tmp) {
          contractId.add(i * 4 + n);
        }
      }
    }
    logger.info(contractId.toString());
  }
}
