package letscode.boot3;


import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Collection;

@Slf4j
public class Boot3Application {

    public static void main(String[] args) {
        var dataSource = new DriverManagerDataSource("jdbc:postgresql://localhost/postgres", "postgres", "postgres");
        dataSource.setDriverClassName(Driver.class.getName());
        var template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();
        var cs = new DefaultCustomerService(template);
        var all = cs.all();
        all.forEach(c -> log.info(c.toString()));
    }

}

@Slf4j
class DefaultCustomerService {

    private final JdbcTemplate template;
    private final RowMapper<Customer> customerRowMapper = (resultSet, rowNum) -> new Customer(resultSet.getInt("id"), resultSet.getString("name"));


    DefaultCustomerService(JdbcTemplate template) {

        this.template = template;
    }

    Collection<Customer> all() {
        return this.template.query("select id, name from customers", this.customerRowMapper);
    }

}

record Customer(Integer id, String name) {

}