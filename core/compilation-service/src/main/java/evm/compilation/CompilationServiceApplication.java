package evm.compilation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@EnableCaching
@ComponentScan(basePackages = {"evm.compilation", "evm.common"})
@EntityScan(basePackages = "evm.compilation")
@EnableJpaRepositories(basePackages = "evm.compilation")
public class CompilationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompilationServiceApplication.class, args);
    }
}