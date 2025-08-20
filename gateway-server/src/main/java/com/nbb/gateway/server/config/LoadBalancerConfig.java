package com.nbb.gateway.server.config;

import com.nbb.gateway.server.framework.loadbalancer.TagPriorityListLoadBalancer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author 胡鹏
 */
@Configuration
public class LoadBalancerConfig {

    @Bean
    public TagPriorityListLoadBalancer tagPriorityListLoadBalancer(Environment environment,
                                                                   LoadBalancerClientFactory loadBalancerClientFactory) {
        String serviceName = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        ObjectProvider<ServiceInstanceListSupplier> objectProvider =
                loadBalancerClientFactory.getLazyProvider(serviceName, ServiceInstanceListSupplier.class);

        return new TagPriorityListLoadBalancer(objectProvider, serviceName);
    }
}
