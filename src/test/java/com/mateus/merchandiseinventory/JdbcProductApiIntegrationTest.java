package com.mateus.merchandiseinventory;

import com.mateus.merchandiseinventory.dao.factory.DaoFactory;
import com.mateus.merchandiseinventory.dao.factory.JdbcDaoFactory;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.persistence.type=jdbc")
class JdbcProductApiIntegrationTest extends ProductApiIntegrationTest {

    @Override
    protected Class<? extends DaoFactory> expectedDaoFactory() {
        return JdbcDaoFactory.class;
    }
}
