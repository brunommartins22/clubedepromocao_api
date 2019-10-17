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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SimpleDatasourceConfiguration {

    private final DataBaseCfg databaseCfg = new DataBaseCfg();
    
    //Inclua a propriedade 'packages.to.scan' no arquivo application.properties
    //para alterar o valor deste atributo
    @Value("${packages.to.scan:br.com.interagese}")
    private String[] packagesToScan;
    
    //Tem que ser criado como bean, para
    //ser injetado com as propriedades do arquivo application.propreties
    @Bean
    public JpaProperties jpaProperties() {
        return new JpaProperties();
    }

    //Configura o postgre como datasource primário
    //e utiliza as configurações do arquivo
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDatasource() {
        return DataSourceBuilder.create()
                .build();
    }

    @Bean(name ="entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, JpaProperties jpaProperties) {

        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        
        hibernateJpaVendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        hibernateJpaVendorAdapter.setShowSql(jpaProperties.isShowSql());
        // emBean.setJpaPropertyMap(.getHibernateProperties(primaryDatasource));
        LocalContainerEntityManagerFactoryBean emBean = new LocalContainerEntityManagerFactoryBean();
        emBean.setDataSource(dataSource);
        emBean.setPackagesToScan(packagesToScan);
        emBean.setJpaVendorAdapter(hibernateJpaVendorAdapter);
        emBean.setJpaPropertyMap(jpaProperties.getHibernateProperties(dataSource));

        return emBean;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(factory);
        return transactionManager;
    }

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
    
    @Bean("integradoEntityManager")
    public LocalContainerEntityManagerFactoryBean integradoEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("integradoDatasource") DataSource dataSource
    ) {
        return builder.dataSource(dataSource)
                .packages("br.com.firebird")
                .persistenceUnit("integradoPU")
                .properties(firebirdProperties())
                .build();
    }

    @Bean("integradoTransaction")
    public PlatformTransactionManager stageTransactionManager(
            @Qualifier("integradoEntityManager") EntityManagerFactory factory
    ) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(factory);
        return transactionManager;
    }
    
    @Bean("multiTransaction")
    public ChainedTransactionManager chainedTransactionManager(
            PlatformTransactionManager postgreTransaction,
            @Qualifier("integradoTransaction") PlatformTransactionManager firebirdTransaction
    ) {
        return new ChainedTransactionManager(postgreTransaction, firebirdTransaction);
    }
    
     private Map<String, ?> firebirdProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("hibernate.dialect", "org.hibernate.dialect.FirebirdDialect");
        return map;
    }

}
