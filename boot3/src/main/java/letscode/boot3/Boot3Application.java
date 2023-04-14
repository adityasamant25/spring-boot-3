package letscode.boot3;


import lombok.extern.slf4j.Slf4j;
import org.postgresql.Driver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.util.Assert;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class Boot3Application {

    public static void main(String[] args) {
        var dataSource = new DriverManagerDataSource("jdbc:postgresql://localhost/postgres", "postgres", "postgres");
        dataSource.setDriverClassName(Driver.class.getName());
        var template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();
        var cs = new DefaultCustomerService(template);

        var aditya = cs.add("Aditya");
        var isha = cs.add("Isha");

        var all = cs.all();
        Assert.state(all.contains(aditya) && all.contains(isha), "We didn't add Aditya and Isha successfully");
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

    Customer add(String name) {
        var al = new ArrayList<Map<String, Object>>();
        al.add(Map.of("id", Long.class));
        var keyHolder = new GeneratedKeyHolder(al);
        this.template.update(con -> {
            var ps = con.prepareStatement("insert into customers (name) values(?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            return ps;
        }, keyHolder);
        var generatedId = Objects.requireNonNull(keyHolder.getKeys()).get("id");
        log.info("generatedId: {}", generatedId);
        Assert.state(generatedId instanceof Number, "the generated id must be a number");
        Number number = (Number) generatedId;
        return byId(number.intValue());
    }

    Customer byId(Integer id) {
        return this.template.queryForObject("select id, name from customers where id = ?", this.customerRowMapper, id);
    }

    Collection<Customer> all() {
        return this.template.query("select id, name from customers", this.customerRowMapper);
    }

}

record Customer(Integer id, String name) {

}