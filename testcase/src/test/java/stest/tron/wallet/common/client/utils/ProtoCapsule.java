package stest.tron.wallet.common.client.utils;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
