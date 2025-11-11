package com.vng.zing.zcm.client.http;

import org.springframework.web.client.RestClient;

/**
 * HTTP API providing load-balanced RestClient for service-to-service calls.
 */
public interface HttpApi {
  
  /**
   * Returns a pre-configured load-balanced RestClient.
   * 
   * @return the RestClient instance
   */
  RestClient client();
}
