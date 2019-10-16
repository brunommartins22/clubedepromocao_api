
package br.com.interagese.promocao.configuration;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class FirebirdLiquibase {
    
    @Autowired()
    @Qualifier("integradoDatasource")
    private DataSource datasource;
    
    @Bean
    @DependsOn(value = "integradoEntityManager")
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:/changelog/changelog-firebird.xml");
        liquibase.setDataSource(datasource);
        liquibase.setShouldRun(true);
        return liquibase;
    }
    
}
