package com.nbb.gateway.server.framework.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;

/**
 * @author 胡鹏
 */
public class TagUtils {

    public static final String TAG_KEY = "tag";

    public static String getTag(HttpHeaders headers) {
        return headers.getFirst(TAG_KEY);
    }

    public static String getTag(ServiceInstance instance) {
        return instance.getMetadata().get(TAG_KEY);
    }
}
