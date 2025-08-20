package com.nbb.gateway.server.framework.loadbalancer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 根据请求header中的tag值优先级选择服务实例
 * @author 胡鹏
 */
@Slf4j
public class TagPriorityListLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    /** 当前所需要调用的服务名 */
    final String serviceId;

    public TagPriorityListLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);

        HttpHeaders headers = ((RequestDataContext)request.getContext()).getClientRequest().getHeaders();
        String tag = TagUtils.getTag(headers);
        return supplier.get().next().map(instances -> this.getInstanceResponse(instances, tag));

    }


    /**
     * 根据tag从服务实例列表中获取匹配的实例响应
     *
     * @param instances 服务实例列表，用于筛选匹配的实例
     * @param requestHeaderTag 标签字符串，用于匹配服务实例的标签
     * @return 返回包装了匹配服务实例的响应对象
     */
    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, String requestHeaderTag) {
        if (instances.isEmpty()) {
            log.warn("服务名为【{}】的服务实例列表为空", requestHeaderTag);
            return new EmptyResponse();
        }

        // 请求header中没有tag，则随机取一个实例
        if (StrUtil.isBlank(requestHeaderTag)) {
            return new DefaultResponse(this.randomInstance(instances));
        }

        // 构建服务tag标识与服务实例的映射关系
        MultiValueMap<String, ServiceInstance> tag2Instances = new LinkedMultiValueMap<>();
        for (ServiceInstance instance : instances) {
            String instanceTag = TagUtils.getTag(instance);
            if (StrUtil.isNotBlank(instanceTag)) {
                tag2Instances.add(instanceTag, instance);
            }
        }

        // 通过tag值匹配对应服务
        List<ServiceInstance> tatMatchInstances = null;
        String[] tagPriorityArray = requestHeaderTag.split(",");
        for (String tagPriority : tagPriorityArray) {
            tatMatchInstances = tag2Instances.get(tagPriority);
            if (CollUtil.isNotEmpty(tatMatchInstances)) {
                break;
            }
        }

        if (CollUtil.isNotEmpty(tatMatchInstances)) {
            return new DefaultResponse(this.randomInstance(tatMatchInstances));
        }

        // 未能通过tag匹配对应实例
        log.info("服务名为【{}】，未匹配到的tag值为【{}】实例，将随机选择一个实例", serviceId, requestHeaderTag);
        return new DefaultResponse(this.randomInstance(instances));
    }

    private ServiceInstance randomInstance(List<ServiceInstance> instances) {
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }
}
