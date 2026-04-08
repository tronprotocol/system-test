package stest.tron.wallet.common.client.utils;

import java.math.BigInteger;

public class EnergyCost {

  private static final long ZERO_TIER = 0;
  private static final long BASE_TIER = 2;
  private static final long VERY_LOW_TIER = 3;
  private static final long LOW_TIER = 5;
  private static final long MID_TIER = 8;
  private static final long HIGH_TIER = 10;
  private static final long EXT_TIER = 20;
  private static final long SPECIAL_TIER = 1;

  private static final long EXP_ENERGY = 10;
  private static final long EXP_BYTE_ENERGY = 10;
  private static final long SHA3 = 30;
  // 3MB
  private static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024);
  private static final long MEMORY = 3;
  private static final long COPY_ENERGY = 3;
  private static final long SHA3_WORD = 6;
  private static final long SLOAD = 50;
  private static final long CLEAR_SSTORE = 5000;
  private static final long SET_SSTORE = 20000;
  private static final long RESET_SSTORE = 5000;
  private static final long LOG_DATA_ENERGY = 8;
  private static final long LOG_ENERGY = 375;
  private static final long LOG_TOPIC_ENERGY = 375;
  private static final long BALANCE = 20;
  private static final long FREEZE = 20000;
  private static final long NEW_ACCT_CALL = 25000;
  private static final long UNFREEZE = 20000;
  private static final long FREEZE_EXPIRE_TIME = 50;
  private static final long VOTE_WITNESS = 30000;
  private static final long WITHDRAW_REWARD = 20000;
  private static final long CREATE = 32000;
  private static final long CALL_ENERGY = 40;
  private static final long VT_CALL = 9000;
  private static final long STIPEND_CALL = 2300;
  private static final long EXT_CODE_COPY = 20;
  private static final long EXT_CODE_SIZE = 20;
  private static final long EXT_CODE_HASH = 400;
  private static final long SUICIDE = 0;
  private static final long STOP = 0;
  private static final long CREATE_DATA = 200;


  public static long getNewAcctCall() {
    return NEW_ACCT_CALL;
  }

  public static long getCreateData() {
    return CREATE_DATA;
  }




}
