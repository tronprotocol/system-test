package stest.tron.wallet.common.client.utils.exception;

public class JsonRpcInvalidParamsException extends TronException {

  public JsonRpcInvalidParamsException() {
    super();
  }

  public JsonRpcInvalidParamsException(String msg) {
    super(msg);
  }

  public JsonRpcInvalidParamsException(String message, Throwable cause) {
    super(message, cause);
  }
}