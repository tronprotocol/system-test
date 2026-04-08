package stest.tron.wallet.common.client.utils.services;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.util.StringUtil;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.CommonParameter;
import stest.tron.wallet.common.client.utils.Hash;
import stest.tron.wallet.common.client.utils.JsonFormat;
import stest.tron.wallet.common.client.utils.Sha256Hash;


@Slf4j(topic = "API")
public class Util {

  public static final String PERMISSION_ID = "Permission_id";
  public static final String VISIBLE = "visible";
  public static final String TRANSACTION = "transaction";
  public static final String VALUE = "value";
  public static final String CONTRACT_TYPE = "contractType";
  public static final String EXTRA_DATA = "extra_data";
  public static final String PARAMETER = "parameter";

  public static String printTransactionFee(String transactionFee) {
    JSONObject jsonObject = new JSONObject();
    JSONObject receipt = JSONObject.parseObject(transactionFee);
    jsonObject.put("Receipt", receipt.get("receipt"));
    return jsonObject.toJSONString();
  }

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getClass() + " : " + e.getMessage());
    return jsonObject.toJSONString();
  }

/*  public static String printBlockList(BlockList list, boolean selfType) {
    List<Block> blocks = list.getBlockList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));
    JSONArray jsonArray = new JSONArray();
    blocks.stream().forEach(block -> jsonArray.add(printBlockToJSON(block, selfType)));
    jsonObject.put("block", jsonArray);

    return jsonObject.toJSONString();
  }*/

/*  public static String printBlock(Block block, boolean selfType) {
    return printBlockToJSON(block, selfType).toJSONString();
  }*/

/*  public static JSONObject printBlockToJSON(Block block, boolean selfType) {

    BlockCapsule blockCapsule = new BlockCapsule(block);
    String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(block, selfType));
    jsonObject.put("blockID", blockID);
    if (!blockCapsule.getTransactions().isEmpty()) {
      jsonObject.put("transactions",
          printTransactionListToJSON(blockCapsule.getTransactions(), selfType));
    }
    return jsonObject;
  }*/


  public static String printTransactionIdList(TransactionIdList list, boolean selfType) {
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));

    return jsonObject.toJSONString();
  }




  public static byte[] generateContractAddress(Transaction trx, byte[] ownerAddress) {
    // get tx hash
    byte[] txRawDataHash = Sha256Hash
        .of(CommonParameter.getInstance().isECKeyCryptoEngine(), trx.getRawData().toByteArray())
        .getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }




  public static boolean getVisible(final HttpServletRequest request) {
    boolean visible = false;
    if (StringUtil.isNotBlank(request.getParameter(VISIBLE))) {
      visible = Boolean.valueOf(request.getParameter(VISIBLE));
    }
    return visible;
  }

  public static boolean getVisiblePost(final String input) {
    boolean visible = false;
    if (StringUtil.isNotBlank(input)) {
      JSONObject jsonObject = JSON.parseObject(input);
      if (jsonObject.containsKey(VISIBLE)) {
        visible = jsonObject.getBoolean(VISIBLE);
      }
    }

    return visible;
  }

  public static String getContractType(final String input) {
    String contractType = null;
    JSONObject jsonObject = JSON.parseObject(input);
    if (jsonObject.containsKey(CONTRACT_TYPE)) {
      contractType = jsonObject.getString(CONTRACT_TYPE);
    }
    return contractType;
  }

  public static String getHexAddress(final String address) {
    if (address != null) {
      byte[] addressByte = WalletClient.decodeFromBase58Check(address);
      return ByteArray.toHexString(addressByte);
    } else {
      return null;
    }
  }

  public static String getHexString(final String string) {
    return ByteArray.toHexString(ByteString.copyFromUtf8(string).toByteArray());
  }

  public static Transaction setTransactionPermissionId(JSONObject jsonObject,
      Transaction transaction) {
    if (jsonObject.containsKey(PERMISSION_ID)) {
      int permissionId = jsonObject.getInteger(PERMISSION_ID);
      return setTransactionPermissionId(permissionId, transaction);
    }

    return transaction;
  }

  public static Transaction setTransactionPermissionId(int permissionId, Transaction transaction) {
    if (permissionId > 0) {
      Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
      Transaction.Contract.Builder contract = raw.getContract(0).toBuilder()
          .setPermissionId(permissionId);
      raw.clearContract();
      raw.addContract(contract);
      return transaction.toBuilder().setRawData(raw).build();
    }

    return transaction;
  }

  public static Transaction setTransactionExtraData(JSONObject jsonObject,
      Transaction transaction, boolean visible) {
    if (jsonObject.containsKey(EXTRA_DATA)) {
      String data = jsonObject.getString(EXTRA_DATA);
      return setTransactionExtraData(data, transaction, visible);
    }

    return transaction;
  }

  public static Transaction setTransactionExtraData(String data, Transaction transaction,
      boolean visible) {
    if (data.length() > 0) {
      Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
      if (visible) {
        raw.setData(ByteString.copyFrom(data.getBytes()));
      } else {
        raw.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));
      }
      return transaction.toBuilder().setRawData(raw).build();
    }

    return transaction;
  }

  public static boolean getVisibleOnlyForSign(JSONObject jsonObject) {
    boolean visible = false;
    if (jsonObject.containsKey(VISIBLE)) {
      visible = jsonObject.getBoolean(VISIBLE);
    } else if (jsonObject.getJSONObject(TRANSACTION).containsKey(VISIBLE)) {
      visible = jsonObject.getJSONObject(TRANSACTION).getBoolean(VISIBLE);
    }
    return visible;
  }

  public static String parseMethod(String methodSign, String input) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    //System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (StringUtils.isEmpty(input)) {
      return Hex.toHexString(selector);
    }

    return Hex.toHexString(selector) + input;
  }

  public static long getJsonLongValue(final JSONObject jsonObject, final String key) {
    return getJsonLongValue(jsonObject, key, false);
  }

  public static long getJsonLongValue(JSONObject jsonObject, String key, boolean required) {
    BigDecimal bigDecimal = jsonObject.getBigDecimal(key);
    if (required && bigDecimal == null) {
      throw new InvalidParameterException("key [" + key + "] does not exist");
    }
    return (bigDecimal == null) ? 0L : bigDecimal.longValueExact();
  }

  public static String getMemo(byte[] memo) {
    int index = memo.length;
    for (; index > 0; --index) {
      if (memo[index - 1] != 0) {
        break;
      }
    }

    byte[] inputCheck = new byte[index];
    System.arraycopy(memo, 0, inputCheck, 0, index);
    return new String(inputCheck, Charset.forName("UTF-8"));
  }

  public static void processError(Exception e, HttpServletResponse response) {
    logger.debug("Exception: {}", e.getMessage());
    try {
      response.getWriter().println(Util.printErrorMsg(e));
    } catch (IOException ioe) {
      logger.debug("IOException: {}", ioe.getMessage());
    }
  }

  public static String convertOutput(Account account) {
    if (account.getAssetIssuedID().isEmpty()) {
      return JsonFormat.printToString(account, false);
    } else {
      JSONObject accountJson = JSONObject.parseObject(JsonFormat.printToString(account, false));
      String assetId = accountJson.get("asset_issued_ID").toString();
      accountJson.put("asset_issued_ID",
          ByteString.copyFrom(ByteArray.fromHexString(assetId)).toStringUtf8());
      return accountJson.toJSONString();
    }
  }

  public static void printAccount(Account reply, HttpServletResponse response, Boolean visible)
      throws IOException {
    if (reply != null) {
      if (visible) {
        response.getWriter().println(JsonFormat.printToString(reply, true));
      } else {
        response.getWriter().println(convertOutput(reply));
      }
    } else {
      response.getWriter().println("{}");
    }
  }



}
