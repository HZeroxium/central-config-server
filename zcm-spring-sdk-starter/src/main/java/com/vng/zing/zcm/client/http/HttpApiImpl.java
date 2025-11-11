package com.vng.zing.zcm.client.http;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class HttpApiImpl implements HttpApi {
  
  private final RestClient.Builder lbRestClientBuilder;
  
  @Override
  public RestClient client() {
    return lbRestClientBuilder.build();
  }
}
