package evm.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableFeignClients(basePackages = "evm.event.client")
@SpringBootApplication
@ComponentScan(basePackages = {"evm.event", "evm.category", "evm.common"})
@EntityScan(basePackages = {"evm.event", "evm.category"})
@EnableJpaRepositories(basePackages = {"evm.event", "evm.category"})
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }

}