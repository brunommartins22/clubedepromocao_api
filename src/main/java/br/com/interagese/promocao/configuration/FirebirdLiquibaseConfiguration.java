
package br.com.interagese.promocao.configuration;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class FirebirdLiquibaseConfiguration {
    
    @Autowired
    @Qualifier("integradoDatasource")
    private DataSource datasource;
    
    @Bean("integradoLiquibase")
    @DependsOn(value = {"integradoEntityManager", "liquibase"})
    public SpringLiquibase liquibaseIntegrado() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:/changelog/changelog-firebird.xml");
        liquibase.setDataSource(datasource);
        liquibase.setShouldRun(true);
        return liquibase;
    }
    
}
