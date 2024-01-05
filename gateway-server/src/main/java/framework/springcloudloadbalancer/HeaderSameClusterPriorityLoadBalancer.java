package framework.springcloudloadbalancer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 根据请求header中cluster的值，优先调用指定cluster的服务
 */
@Slf4j
public class HeaderSameClusterPriorityLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final String HEADER_CLUSTER_NAME = "cluster";

    private static Random random = new Random();


    final String serviceId;

    ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    public HeaderSameClusterPriorityLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);

        String headerClusterName = this.getHeaderClusterName(request);

        return supplier.get(request).next()
                .map(instances -> this.getInstanceResponse(instances, headerClusterName));
    }

    private String getHeaderClusterName(Request request) {
        RequestDataContext context = (RequestDataContext) request.getContext();
        return context.getClientRequest().getHeaders().getFirst(HEADER_CLUSTER_NAME);
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, String headerClusterName) {
        if (instances.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("No servers available for service: " + serviceId);
            }
            return new EmptyResponse();
        }

        if (StrUtil.isBlank(headerClusterName)) {
            return new DefaultResponse(instances.get(random.nextInt(instances.size())));
        }


        // 集群名称相同的服务实例
        List<NacosServiceInstance> sameClusterServers = instances.stream()
                .map(instance -> (NacosServiceInstance)instance)
                .filter(instance -> headerClusterName.equals(instance.getMetadata().get("nacos.cluster")))
                .collect(Collectors.toList());


        if (!CollectionUtils.isEmpty(sameClusterServers)) {
            return new DefaultResponse(sameClusterServers.get(random.nextInt(sameClusterServers.size())));
        }

        return new DefaultResponse(instances.get(random.nextInt(instances.size())));
    }
}
