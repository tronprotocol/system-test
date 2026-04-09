package tron.trident.client;

import io.grpc.*;

public class HeaderClientInterceptor implements ClientInterceptor {
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(Metadata.Key.of("content-length", Metadata.ASCII_STRING_MARSHALLER), "5");
        headers
            .put(Metadata.Key.of("Content-Type", Metadata.ASCII_STRING_MARSHALLER), "application/grpc");
        headers
            .put(Metadata.Key.of("Host", Metadata.ASCII_STRING_MARSHALLER), "grpc.demo.com");
        headers
            .put(Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER), "testGroupAutoTest");
        headers
            .put(Metadata.Key.of("x-trace-path", Metadata.ASCII_STRING_MARSHALLER), "123 23123");
        headers
            .put(Metadata.Key.of("x-trace-name", Metadata.ASCII_STRING_MARSHALLER), "!@^&$!* ()^&%");
        headers.put(Metadata.Key.of("my-header", Metadata.ASCII_STRING_MARSHALLER), "header-value");
        super.start(responseListener, headers);
      }
    };
  }

}
