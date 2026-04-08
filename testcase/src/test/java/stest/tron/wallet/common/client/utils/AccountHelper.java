package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AccountContract.SetAccountIdContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.BlockCapsule.BlockId;

@Slf4j
/** Helper for account-related operations: queries, creation, permissions, and account resources. */
public class AccountHelper {

  // ---------------------------------------------------------------------------
  // Query account
  // ---------------------------------------------------------------------------

  /** Query an account by its raw address bytes via the full node. */
  public static Account queryAccountByAddress(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** Query an account by its raw address bytes via the full node. */
  public static Account queryAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** Query an account by private key string via the full node. */
  public static Protocol.Account queryAccount(
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = PublicMethod.loadPubKey(); // 04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return PublicMethod.grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  /** Query an account by its raw address bytes via the solidity node. */
  public static Account queryAccount(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** Query an account by its account ID string via the full node. */
  public static Account getAccountById(
      String accountId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
    Account request = Account.newBuilder().setAccountId(bsAccountId).build();
    return blockingStubFull.getAccountById(request);
  }

  /** Query an account by its account ID string via the solidity node. */
  public static Account getAccountByIdFromSolidity(
      String accountId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
    Account request = Account.newBuilder().setAccountId(bsAccountId).build();
    return blockingStubFull.getAccountById(request);
  }

  /** Get the account net (bandwidth) message for the given address. */
  public static AccountNetMessage getAccountNet(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountNet(request);
  }

  /** Get the account resource message (energy, bandwidth, etc.) for the given address. */
  public static AccountResourceMessage getAccountResource(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountResource(request);
  }

  /** Get the account balance at a specific block via the light-node balance API. */
  public static Long getAccountBalance(
      Protocol.Block block, byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    final Long blockNum = block.getBlockHeader().getRawData().getNumber();
    BlockId blockId =
        new BlockId(
            Sha256Hash.of(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                block.getBlockHeader().getRawData().toByteArray()),
            block.getBlockHeader().getRawData().getNumber());

    BalanceContract.AccountIdentifier accountIdentifier =
        BalanceContract.AccountIdentifier.newBuilder()
            .setAddress(ByteString.copyFrom(address))
            .build();
    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setHash(blockId.getByteString())
            .setNumber(blockNum)
            .build();

    BalanceContract.AccountBalanceRequest accountBalanceRequest =
        BalanceContract.AccountBalanceRequest.newBuilder()
            .setAccountIdentifier(accountIdentifier)
            .setBlockIdentifier(blockIdentifier)
            .build();
    return blockingStubFull.getAccountBalance(accountBalanceRequest).getBalance();
  }

  // ---------------------------------------------------------------------------
  // Create / update account
  // ---------------------------------------------------------------------------

  /** Create a new account on-chain and return the transaction ID. */
  public static String createAccountGetTxid(
      byte[] ownerAddress,
      byte[] newAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    Transaction transaction = blockingStubFull.createAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Create a new account on-chain and return whether it succeeded. */
  public static boolean createAccount(
      byte[] ownerAddress,
      byte[] newAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    Transaction transaction = blockingStubFull.createAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Create a new account on-chain via createAccount2 and return the gRPC Return. */
  public static Return createAccount2(
      byte[] ownerAddress,
      byte[] newAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.createAccount2(contract);

    if (transactionExtention == null) {
      return transactionExtention.getResult();
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    } else {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transactionExtention.getResult();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return response;
    }
    return ret;
  }

  /** Update an account name on-chain and return whether it succeeded. */
  public static boolean updateAccount(
      byte[] addressBytes,
      byte[] accountNameBytes,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountUpdateContract.Builder builder = AccountUpdateContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(addressBytes);
    ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(basAddreess);

    AccountUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateAccount(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("Please check!!! transaction == null");
      return false;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Set an account ID on-chain and return whether it succeeded. */
  public static boolean setAccountId(
      byte[] accountIdBytes,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SetAccountIdContract.Builder builder = SetAccountIdContract.newBuilder();
    ByteString bsAddress = ByteString.copyFrom(owner);
    ByteString bsAccountId = ByteString.copyFrom(accountIdBytes);
    builder.setAccountId(bsAccountId);
    builder.setOwnerAddress(bsAddress);
    SetAccountIdContract contract = builder.build();
    Transaction transaction = blockingStubFull.setAccountId(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  // ---------------------------------------------------------------------------
  // Account permission update
  // ---------------------------------------------------------------------------

  /** Parse a JSON object into a Protocol.Permission protobuf message. */
  private static Permission json2Permission(JSONObject json) {
    Permission.Builder permissionBuilder = Permission.newBuilder();
    if (json.containsKey("type")) {
      int type = json.getInteger("type");
      permissionBuilder.setTypeValue(type);
    }
    if (json.containsKey("permission_name")) {
      String permissionName = json.getString("permission_name");
      permissionBuilder.setPermissionName(permissionName);
    }
    if (json.containsKey("threshold")) {
      // long threshold = json.getLong("threshold");
      long threshold = Long.parseLong(json.getString("threshold"));
      permissionBuilder.setThreshold(threshold);
    }
    if (json.containsKey("parent_id")) {
      int parentId = json.getInteger("parent_id");
      permissionBuilder.setParentId(parentId);
    }
    if (json.containsKey("operations")) {
      byte[] operations = ByteArray.fromHexString(json.getString("operations"));
      permissionBuilder.setOperations(ByteString.copyFrom(operations));
    }
    if (json.containsKey("keys")) {
      JSONArray keys = json.getJSONArray("keys");
      List<Key> keyList = new ArrayList<>();
      for (int i = 0; i < keys.size(); i++) {
        Key.Builder keyBuilder = Key.newBuilder();
        JSONObject key = keys.getJSONObject(i);
        String address = key.getString("address");
        long weight = Long.parseLong(key.getString("weight"));
        // long weight = key.getLong("weight");
        // keyBuilder.setAddress(ByteString.copyFrom(address.getBytes()));
        keyBuilder.setAddress(ByteString.copyFrom(WalletClient.decodeFromBase58Check(address)));
        keyBuilder.setWeight(weight);
        keyList.add(keyBuilder.build());
      }
      permissionBuilder.addAllKeys(keyList);
    }
    return permissionBuilder.build();
  }

  /** Update account permissions (owner, witness, actives) using a JSON descriptor and multi-sig keys. */
  public static boolean accountPermissionUpdate(
      String permissionJson,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] priKeys) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
    JSONArray activepermissions = permissions.getJSONArray("active_permissions");

    if (ownerpermission != null) {
      Permission ownerPermission = json2Permission(ownerpermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesspermission != null) {
      Permission witnessPermission = json2Permission(witnesspermission);
      builder.setWitness(witnessPermission);
    }
    if (activepermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activepermissions.size(); j++) {
        JSONObject permission = activepermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Update account permissions and return the full gRPC Return response. */
  public static GrpcAPI.Return accountPermissionUpdateForResponse(
      String permissionJson,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
    JSONArray activepermissions = permissions.getJSONArray("active_permissions");

    if (ownerpermission != null) {
      Permission ownerPermission = json2Permission(ownerpermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesspermission != null) {
      Permission witnessPermission = json2Permission(witnesspermission);
      builder.setWitness(witnessPermission);
    }
    if (activepermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activepermissions.size(); j++) {
        JSONObject permission = activepermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return ret;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response;
  }

  // ---------------------------------------------------------------------------
  // Send coin (TRX transfer)
  // ---------------------------------------------------------------------------

  /** Transfer TRX from owner to recipient and return whether it succeeded. */
  public static Boolean sendcoin(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Integer times = 0;
    while (times++ <= 2) {

      TransferContract.Builder builder = TransferContract.newBuilder();
      ByteString bsTo = ByteString.copyFrom(to);
      ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction ==null");
        continue;
      }
      transaction = PublicMethod.signTransaction(ecKey, transaction);
      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /** Transfer TRX with an appended script payload of the given length. */
  public static Boolean sendcoinWithScript(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      int scriptLength,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Integer times = 0;
    while (times++ <= 2) {

      TransferContract.Builder builder = TransferContract.newBuilder();
      ByteString bsTo = ByteString.copyFrom(to);
      ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
      builder1.setScripts(ByteString.copyFrom(new byte[scriptLength]));
      Transaction.Builder builder2 = transaction.toBuilder();
      builder2.setRawData(builder1);
      transaction = builder2.build();
      System.out.println(transaction.getSerializedSize());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction ==null");
        continue;
      }
      transaction = PublicMethod.signTransaction(ecKey, transaction);
      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /** Transfer TRX and return the signed transaction as a hex string. */
  public static String sendcoinGetTransactionHex(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Integer times = 0;
    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    logger.info(
        "HEX transaction is : "
            + "transaction hex string is "
            + ByteArray.toHexString(transaction.toByteArray()));
    return ByteArray.toHexString(transaction.toByteArray());
  }

  /** Transfer TRX with a delay (in seconds) and return whether it succeeded. */
  public static Boolean sendcoinDelayed(
      byte[] to,
      long amount,
      long delaySeconds,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

    // transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return false;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    logger.info(
        "Txid is "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Transfer TRX with a delay (in seconds) and return the transaction ID. */
  public static String sendcoinDelayedGetTxid(
      byte[] to,
      long amount,
      long delaySeconds,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

    // transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    logger.info(
        "Txid is "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
  }

  /** Transfer TRX via createTransaction2 and return the gRPC Return. */
  public static Return sendcoin2(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.createTransaction2(contract);
    if (transactionExtention == null) {
      return transactionExtention.getResult();
    }

    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    } else {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
    }

    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transactionExtention.getResult();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      //      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return response;
    }
    return ret;
  }

  /** Transfer TRX and return the transaction ID string, delegating to sendcoinWithMemoGetTransactionId. */
  public static String sendcoinGetTransactionId(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull
  ) {
    return sendcoinWithMemoGetTransactionId(to,amount,null,owner,
        priKey,blockingStubFull);
}

  /** Transfer TRX with an optional memo and return the transaction ID string. */
  public static String sendcoinWithMemoGetTransactionId(
      byte[] to,
      long amount,
      String memo,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    // Test raw data
    if(null != memo) {
      Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
      builder1.setData(ByteString.copyFromUtf8(memo));
      Transaction.Builder builder2 = transaction.toBuilder();
      builder2.setRawData(builder1);
      transaction = builder2.build();
    }


    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Transfer TRX with a constructed data payload and return the transaction ID. */
  public static String sendcoinGetTransactionIdForConstructData(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    // Test raw data
    Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
    StringBuffer stringBuffer = new StringBuffer();
    while (stringBuffer.length() <= 20) {
      stringBuffer.append("12345678");
    }
    builder1.setData(ByteString.copyFromUtf8(stringBuffer.toString()));
    Transaction.Builder builder2 = transaction.toBuilder();
    builder2.setRawData(builder1);
    transaction = builder2.build();

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Transfer TRX via createTransaction2 and return the gRPC Return without broadcasting. */
  public static Return sendcoinForReturn(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention transaction = blockingStubFull.createTransaction2(contract);
    if (transaction == null) {
      return transaction.getResult();
    }
    Return ret = transaction.getResult();
    return ret;
  }

  /** Transfer TRX via createTransaction2 and return the unsigned Transaction object. */
  public static Transaction sendcoinForTransaction(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention extention = blockingStubFull.createTransaction2(contract);
    Protocol.Transaction transaction = extention.getTransaction();
    return transaction;
  }
}
