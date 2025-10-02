package com.vng.zing.zcm.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

public interface ClientApi {

  /** Lấy một giá trị cấu hình (đọc từ Environment hiện tại) */
  String get(String key);

  /** Trả về tất cả các cặp key/value theo prefix (đơn giản để debug) */
  Map<String, Object> getAll(String prefix);

  /** Hash cấu hình hiện đang áp dụng (phục vụ drift) */
  String configHash();

  /** Danh sách instance healthy (theo Discovery provider hiện tại) */
  List<ServiceInstance> instances(String serviceName);

  /** Chọn 1 instance theo LB policy (RR). Trả về null nếu không có instance. */
  ServiceInstance choose(String serviceName);

  /** WebClient LB để gọi `http://{service}/path` */
  WebClient http();

  /** Gửi ping ngay (đồng bộ, swallow errors) */
  void pingNow();
}
