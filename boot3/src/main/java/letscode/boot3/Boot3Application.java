package letscode.boot3;


import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.postgresql.Driver;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class Boot3Application {


    public static void main(String[] args) {

        var applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(DataConfiguration.class);
        applicationContext.refresh();
        applicationContext.start();

        var cs = applicationContext.getBean(CustomerService.class);

        var aditya = cs.add("Aditya");
        var isha = cs.add("Isha");

        var all = cs.all();
        Assert.state(all.contains(aditya) && all.contains(isha), "We didn't add Aditya and Isha successfully");
        all.forEach(c -> log.info(c.toString()));
    }

}

@Configuration
class DataConfiguration {

    private static CustomerService transactionalCustomerService(TransactionTemplate tt, CustomerService delegate) {

        var pfb = new ProxyFactoryBean();
        pfb.setTarget(delegate);
        pfb.setProxyTargetClass(true);
        pfb.addAdvice((MethodInterceptor) invocation -> {
            var method = invocation.getMethod();
            var args = invocation.getArguments();
            return tt.execute(status -> {
                try {
                    return method.invoke(delegate, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        return (CustomerService) pfb.getObject();
    }

    @Bean
    CustomerService defaultCustomerService(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate) {
        return transactionalCustomerService(transactionTemplate, new CustomerService(jdbcTemplate));
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager ptm) {
        return new TransactionTemplate(ptm);
    }

    @Bean
    DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    DriverManagerDataSource dataSource() {
        var dataSource = new DriverManagerDataSource("jdbc:postgresql://localhost/postgres", "postgres", "postgres");
        dataSource.setDriverClassName(Driver.class.getName());
        return dataSource;
    }
}

@Slf4j
class CustomerService {

    private final JdbcTemplate template;
    private final RowMapper<Customer> customerRowMapper = (resultSet, rowNum) -> new Customer(resultSet.getInt("id"), resultSet.getString("name"));


    CustomerService(JdbcTemplate template) {

        this.template = template;
    }

    public Customer add(String name) {
        var al = new ArrayList<Map<String, Object>>();
        al.add(Map.of("id", Long.class));
        var keyHolder = new GeneratedKeyHolder(al);
        template.update(con -> {
            var ps = con.prepareStatement("""
                    insert into customers (name) values(?)
                    on conflict on constraint customers_name_key do update set name = excluded.name
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            return ps;
        }, keyHolder);
        var generatedId = Objects.requireNonNull(keyHolder.getKeys()).get("id");
        log.info("generatedId: {}", generatedId);
        Assert.state(generatedId instanceof Number, "the generated id must be a number");
        Number number = (Number) generatedId;
        return byId(number.intValue());
    }

    public Customer byId(Integer id) {
        return template.queryForObject("select id, name from customers where id = ?", customerRowMapper, id);

    }

    public Collection<Customer> all() {
        return template.query("select id, name from customers", customerRowMapper);

    }

}

record Customer(Integer id, String name) {

}