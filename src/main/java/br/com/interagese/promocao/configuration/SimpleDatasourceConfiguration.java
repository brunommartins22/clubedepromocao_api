/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.interagese.promocao.configuration;

import br.com.interagese.erplibrary.Utils;
import br.com.interagese.padrao.rest.util.DataBaseCfg;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SimpleDatasourceConfiguration {

    private final DataBaseCfg databaseCfg = new DataBaseCfg();

    @Bean
    public JpaProperties jpaProperties() {
        return new JpaProperties();
    }

    @Primary
    @Bean("integradoDatasource")
    public DataSource integradoDatasource() {

        System.out.println("Iniciando datasource integrado...");
        return DataSourceBuilder.create()
                .driverClassName("org.firebirdsql.jdbc.FBDriver")
                .url(Utils.convertFilepathToJdbcUrl(databaseCfg.getProperties().get("BDPRINCIPAL")))
                .username("SYSDBA")
                .password("masterkey")
                .build();

    }

    @Primary
    @Bean("integradoTransaction")
    public PlatformTransactionManager stageTransactionManager(
            EntityManagerFactory factory
    ) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(factory);
        return transactionManager;
    }
    
    @Primary
    @Bean("integradoEntityManager")
    public LocalContainerEntityManagerFactoryBean integradoEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("integradoDatasource") DataSource dataSource
    ) {
        return builder.dataSource(dataSource)
                .packages("br.com.interagese.firebird")
                .persistenceUnit("integradoPU")
                .properties(firebirdProperties())
                .build();
    }
    
    private Map<String, ?> firebirdProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("hibernate.dialect", "org.hibernate.dialect.FirebirdDialect");
        return map;
    }

}
