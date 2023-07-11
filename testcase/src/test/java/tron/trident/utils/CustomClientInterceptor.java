package tron.trident.utils;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class CustomClientInterceptor implements ClientInterceptor {
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    System.out.println("interceptCall test logger");
    return new CustomForwardingClientCall<>(next.newCall(method, callOptions));
  }
}

@Slf4j
class CustomCallListener<RespT> extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {

  protected CustomCallListener(ClientCall.Listener<RespT> delegate) {
    super(delegate);
  }
}

@Slf4j
class CustomForwardingClientCall<ReqT, RespT> extends ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT> {

  protected CustomForwardingClientCall(ClientCall<ReqT, RespT> delegate) {
    super(delegate);
  }

  @Override
  protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
    CustomCallListener<RespT> listener = new CustomCallListener<>(responseListener);
    System.out.println("check start");
    delegate().start(listener, headers);
  }
}

