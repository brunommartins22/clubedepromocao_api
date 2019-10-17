
package br.com.interagese;

import br.com.interagese.padrao.rest.util.configuration.MultiDatasourceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MultiDatasourceConfiguration.class}))
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
