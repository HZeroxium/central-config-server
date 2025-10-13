package com.vng.zing.zcm.client;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
class HttpApiImpl implements HttpApi {
  
  private final RestClient.Builder lbRestClientBuilder;
  
  @Override
  public RestClient client() {
    return lbRestClientBuilder.build();
  }
}
