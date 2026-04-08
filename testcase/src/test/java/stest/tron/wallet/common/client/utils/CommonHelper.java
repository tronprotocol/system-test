package stest.tron.wallet.common.client.utils;

import com.google.common.primitives.Longs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import stest.tron.wallet.common.client.WalletClient;

@Slf4j
/** Helper for common utilities: address/key conversion, encoding, and general-purpose operations. */
public class CommonHelper {

  // ---------------------------------------------------------------------------
  // Address / Key helpers
  // ---------------------------------------------------------------------------

  /** Derives the TRON address byte array from a hex-encoded private key. */
  public static byte[] getFinalAddress(String priKey) {
    WalletClient walletClient;
    walletClient = new WalletClient(priKey);
    // walletClient.init(0);
    return walletClient.getAddress();
  }

  /** Returns the raw address bytes from an ECKey instance. */
  public static byte[] getAddress(ECKey ecKey) {

    return ecKey.getAddress();
  }

  /** Logs the private key, hex address, and Base58Check address for debugging. */
  public static boolean printAddress(String key) {
    // Wallet.setAddressPreFixByte()();
    logger.info(key);
    logger.info(ByteArray.toHexString(getFinalAddress(key)));
    logger.info(Base58.encode58Check(getFinalAddress(key)));
    return true;
  }

  /** Returns the Base58Check-encoded address string for a given private key. */
  public static String getAddressString(String key) {
    // Wallet.setAddressPreFixByte()();
    return Base58.encode58Check(getFinalAddress(key));
  }

  /** Returns a list containing [privateKey, hexAddress, base58Address] for a given key. */
  public static ArrayList<String> getAddressInfo(String key) {
    // Wallet.setAddressPreFixByte()();
    ArrayList<String> accountList = new ArrayList<String>();
    accountList.add(key);
    accountList.add(ByteArray.toHexString(getFinalAddress(key)));
    accountList.add(Base58.encode58Check(getFinalAddress(key)));
    return accountList;
  }

  /** Loads a stub public key string from a fixed-size char buffer. */
  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  // ---------------------------------------------------------------------------
  // Encoding / Decoding
  // ---------------------------------------------------------------------------

  /** Decodes a Base58Check-encoded string and verifies its checksum, returning the payload bytes. */
  public static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), decodeData);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), hash0);
    if (hash1[0] == decodeCheck[decodeData.length]
        && hash1[1] == decodeCheck[decodeData.length + 1]
        && hash1[2] == decodeCheck[decodeData.length + 2]
        && hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // String / Byte conversion helpers
  // ---------------------------------------------------------------------------

  /** Converts a list of parameter objects into a comma-separated JSON-style argument string. */
  public static String parametersString(List<Object> parameters) {
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
    return input;
  }

  /** Converts a Java string into its hexadecimal character-code representation. */
  public static String stringToHexString(String s) {
    String str = "";
    for (int i = 0; i < s.length(); i++) {
      int ch = s.charAt(i);
      String s4 = Integer.toHexString(ch);
      str = str + s4;
    }
    return str;
  }

  /** Converts a hexadecimal string back into a human-readable string using GBK encoding. */
  public static String hexStringToString(String s) {
    if (s == null || s.equals("")) {
      return null;
    }
    s = s.replace(" ", "");
    byte[] baKeyword = new byte[s.length() / 2];
    for (int i = 0; i < baKeyword.length; i++) {
      try {
        baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      s = new String(baKeyword, "gbk");
      new String();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return s;
  }

  /** Strips all trailing "00" hex pairs from a hex string. */
  public static String removeAll0sAtTheEndOfHexStr(String s) {
    return s.replaceAll("(00)+$", "");
  }

  /** Converts a byte array into a decimal-digit string representation. */
  public static String bytes32ToString(byte[] bytes) {
    if (bytes == null) {
      return "null";
    }
    int imax = bytes.length - 1;
    if (imax == -1) {
      return "";
    }

    StringBuilder b = new StringBuilder();
    for (int i = 0; ; i++) {
      b.append(bytes[i]);
      if (i == imax) {
        return b.toString();
      }
    }
  }

  /** Converts a byte sub-array into an uppercase hex string. */
  public static String byte2HexStr(byte[] b, int offset, int length) {
    StringBuilder ssBuilder = new StringBuilder();
    for (int n = offset; n < offset + length && n < b.length; n++) {
      String stmp = Integer.toHexString(b[n] & 0xFF);
      ssBuilder.append((stmp.length() == 1) ? "0" + stmp : stmp);
    }
    return ssBuilder.toString().toUpperCase().trim();
  }

  /** Splits a byte array into a list of 32-byte hex-encoded chunks. */
  public static List<String> getStrings(byte[] data) {
    int index = 0;
    List<String> ret = new ArrayList<>();
    while (index < data.length) {
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
  }

  /** Extracts the ABI-encoded string message from a contract return byte array. */
  public static String getContractStringMsg(byte[] contractMsgArray) {
    int resultLenth = ByteArray.toInt(ByteArray.subArray(contractMsgArray, 32, 64));
    return ByteArray.toStr(ByteArray.subArray(contractMsgArray, 64, 64 + resultLenth));
  }

  // ---------------------------------------------------------------------------
  // ABI JSON parsing
  // ---------------------------------------------------------------------------

  /** Parses a JSON ABI string into a SmartContract.ABI protobuf (fallback-aware). */
  public static SmartContract.ABI jsonStr2Abi(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous =
          abiItem.getAsJsonObject().get("anonymous") != null
              && abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      final boolean constant =
          abiItem.getAsJsonObject().get("constant") != null
              && abiItem.getAsJsonObject().get("constant").getAsBoolean();
      final String name =
          abiItem.getAsJsonObject().get("name") != null
              ? abiItem.getAsJsonObject().get("name").getAsString()
              : null;
      JsonArray inputs =
          abiItem.getAsJsonObject().get("inputs") != null
              ? abiItem.getAsJsonObject().get("inputs").getAsJsonArray()
              : null;
      final JsonArray outputs =
          abiItem.getAsJsonObject().get("outputs") != null
              ? abiItem.getAsJsonObject().get("outputs").getAsJsonArray()
              : null;
      String type =
          abiItem.getAsJsonObject().get("type") != null
              ? abiItem.getAsJsonObject().get("type").getAsString()
              : null;
      final boolean payable =
          abiItem.getAsJsonObject().get("payable") != null
              && abiItem.getAsJsonObject().get("payable").getAsBoolean();
      final String stateMutability =
          abiItem.getAsJsonObject().get("stateMutability") != null
              ? abiItem.getAsJsonObject().get("stateMutability").getAsString()
              : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (inputs != null) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null
              || inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = inputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null
              || outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder =
              SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = outputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  /** Parses a JSON ABI string into a SmartContract.ABI protobuf (fallback+receive aware). */
  public static SmartContract.ABI jsonStr2Abi2(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous =
          abiItem.getAsJsonObject().get("anonymous") != null
              && abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      final boolean constant =
          abiItem.getAsJsonObject().get("constant") != null
              && abiItem.getAsJsonObject().get("constant").getAsBoolean();
      final String name =
          abiItem.getAsJsonObject().get("name") != null
              ? abiItem.getAsJsonObject().get("name").getAsString()
              : null;
      JsonArray inputs =
          abiItem.getAsJsonObject().get("inputs") != null
              ? abiItem.getAsJsonObject().get("inputs").getAsJsonArray()
              : null;
      final JsonArray outputs =
          abiItem.getAsJsonObject().get("outputs") != null
              ? abiItem.getAsJsonObject().get("outputs").getAsJsonArray()
              : null;
      String type =
          abiItem.getAsJsonObject().get("type") != null
              ? abiItem.getAsJsonObject().get("type").getAsString()
              : null;
      final boolean payable =
          abiItem.getAsJsonObject().get("payable") != null
              && abiItem.getAsJsonObject().get("payable").getAsBoolean();
      final String stateMutability =
          abiItem.getAsJsonObject().get("stateMutability") != null
              ? abiItem.getAsJsonObject().get("stateMutability").getAsString()
              : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback")
          && !type.equalsIgnoreCase("receive")
          && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (inputs != null) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null
              || inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = inputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null
              || outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder =
              SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = outputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }
      entryBuilder.setType(getEntryType2(type));

      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  /** Maps an ABI entry type string to the corresponding protobuf EntryType enum value. */
  public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      case "error":
        return SmartContract.ABI.Entry.EntryType.Error;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  /** Maps an ABI entry type string to the protobuf EntryType enum, including the Receive type. */
  public static SmartContract.ABI.Entry.EntryType getEntryType2(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      case "receive":
        return SmartContract.ABI.Entry.EntryType.Receive;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  /** Maps a state mutability string to the corresponding protobuf StateMutabilityType enum. */
  public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  // ---------------------------------------------------------------------------
  // Contract address helpers
  // ---------------------------------------------------------------------------

  /** Generates a contract address by hashing the transaction raw data combined with the owner address. */
  public static byte[] generateContractAddress(Transaction trx, byte[] owneraddress) {

    // get owner address
    // this address should be as same as the onweraddress in trx, DONNOT modify it
    byte[] ownerAddress = owneraddress;

    // get tx hash
    byte[] txRawDataHash =
        Sha256Hash.of(
                CommonParameter.getInstance().isECKeyCryptoEngine(), trx.getRawData().toByteArray())
            .getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  /** Computes a CREATE2 contract address from [base58Address, hexCode, salt] parameters. */
  public static String create2(String[] parameters) {
    if (parameters == null || parameters.length != 3) {
      logger.error("create2 needs 3 parameter:\ncreate2 address code salt");
      return null;
    }

    byte[] address = WalletClient.decodeFromBase58Check(parameters[0]);
    if (!WalletClient.addressValid(address)) {
      logger.error("length of address must be 21 bytes.");
      return null;
    }

    byte[] code = Hex.decode(parameters[1]);
    byte[] temp = Longs.toByteArray(Long.parseLong(parameters[2]));
    if (temp.length != 8) {
      logger.error("Invalid salt!");
      return null;
    }
    byte[] salt = new byte[32];
    System.arraycopy(temp, 0, salt, 24, 8);

    byte[] mergedData = ByteUtil.merge(address, salt, Hash.sha3(code));
    String create2Address = Base58.encode58Check(Hash.sha3omit12(mergedData));

    logger.info("create2 Address: " + create2Address);

    return create2Address;
  }

  /** Replaces library placeholder patterns (__$...$__) in bytecode with the given address. */
  public static String replaceCode(String code, String address) {
    if (code.indexOf("__$") == -1) {
      return code;
    } else {
      int index = code.indexOf("_");
      String oldStr = code.substring(index - 1, index + 39);
      Pattern p = Pattern.compile(oldStr);
      Matcher m = p.matcher(code);
      String result = m.replaceAll(address);
      return result;
    }
  }

  // ---------------------------------------------------------------------------
  // Transaction formatting
  // ---------------------------------------------------------------------------

  /** Formats a transaction's hash, txid, and raw data timestamp into a readable string. */
  public static String printTransaction(Transaction transaction) {
    String result = "";
    result += "hash: ";
    result += "\n";
    result +=
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(), transaction.toByteArray()));
    result += "\n";
    result += "txid: ";
    result += "\n";
    result +=
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
    result += "\n";

    if (transaction.getRawData() != null) {
      result += "raw_data: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printTransactionRow(transaction.getRawData());
      result += "}";
      result += "\n";
    }

    return result;
  }

  /** Extracts and returns the timestamp from a transaction's raw data. */
  public static long printTransactionRow(Transaction.raw raw) {
    long timestamp = raw.getTimestamp();

    return timestamp;
  }

  // ---------------------------------------------------------------------------
  // File I/O and process execution
  // ---------------------------------------------------------------------------

  /** Reads a file, returning either its first line or a parsed library link path. */
  public static String fileRead(String filePath, boolean isLibrary) throws Exception {
    File file = new File(filePath);
    FileReader reader = new FileReader(file);
    BufferedReader breader = new BufferedReader(reader);
    StringBuilder sb = new StringBuilder();
    String s = "";
    if (!isLibrary) {
      if ((s = breader.readLine()) != null) {
        sb.append(s);
      }
      breader.close();
    } else {
      String fistLine = breader.readLine();
      breader.readLine();
      if ((s = breader.readLine()) != null && !s.equals("")) {
        s = s.substring(s.indexOf("-> ") + 3);
        sb.append(s + ":");
      } else {
        s = fistLine.substring(fistLine.indexOf("__") + 2, fistLine.lastIndexOf("__"));
        sb.append(s + ":");
      }
      breader.close();
    }
    return sb.toString();
  }

  /** Executes a shell command and returns stdout or stderr, whichever is longer. */
  public synchronized static String exec(String command) throws InterruptedException {
    String returnString = "";
    String errReturnString = "";
    Process pro = null;
    Runtime runTime = Runtime.getRuntime();
    if (runTime == null) {
      logger.error("Create runtime false!");
    }
    try {
      pro = runTime.exec(command);
      BufferedReader input = new BufferedReader(new InputStreamReader(pro.getInputStream()));
      PrintWriter output = new PrintWriter(new OutputStreamWriter(pro.getOutputStream()));
      String line;
      while ((line = input.readLine()) != null) {
        returnString = returnString + line + "\n";
      }
      InputStream stderr = pro.getErrorStream();
      InputStreamReader errReader = new InputStreamReader(stderr);
      BufferedReader br = new BufferedReader(errReader);
      String errLine = null;
      while ((errLine = br.readLine()) != null) {
        errReturnString = errReturnString + errLine + "\n";
      }
      input.close();
      output.close();
      errReader.close();
      br.close();
      pro.destroy();
    } catch (IOException ex) {
      logger.error(null, ex);
    }
    return returnString.length() >= errReturnString.length() ? returnString : errReturnString;
  }
}
