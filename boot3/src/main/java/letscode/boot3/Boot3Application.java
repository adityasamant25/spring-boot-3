package letscode.boot3;


import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import org.postgresql.Driver;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class Boot3Application {

    public static void main(String[] args) {
        var cs = new DefaultCustomerService();
        var all = cs.all();
        all.forEach(c -> log.info(c.toString()));
    }

}

@Slf4j
class DefaultCustomerService {

    private final DataSource dataSource;


    DefaultCustomerService() {
        var dataSource = new DriverManagerDataSource("jdbc:postgresql://localhost/postgres", "postgres", "postgres");
        dataSource.setDriverClassName(Driver.class.getName());
        this.dataSource = dataSource;
    }

    Collection<Customer> all() {
        var listOfCustomers = new ArrayList<Customer>();
        try {
            try (var connection = this.dataSource.getConnection()) {
                try (var stmt = connection.createStatement()) {
                    try (var resultSet = stmt.executeQuery("select * from customers")) {
                        while (resultSet.next()) {
                            var name = resultSet.getString("name");
                            var id = resultSet.getInt("id");
                            listOfCustomers.add(new Customer(id, name));
                        }
                    }
                }
            }
        } catch (Exception th) {
            log.error("something went terribly wrong, but search me, I have no idea what!", th);
        }

        return listOfCustomers;
    }

}

record Customer(Integer id, String name) {

}