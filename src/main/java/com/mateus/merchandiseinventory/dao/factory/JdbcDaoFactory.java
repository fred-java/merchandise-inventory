package com.mateus.merchandiseinventory.dao.factory;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.jdbc.JdbcProductDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.persistence.type", havingValue = "jdbc")
public class JdbcDaoFactory implements DaoFactory {

    private final JdbcClient jdbcClient;

    public JdbcDaoFactory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ProductDao createProductDao() {
        return new JdbcProductDao(jdbcClient);
    }
}
