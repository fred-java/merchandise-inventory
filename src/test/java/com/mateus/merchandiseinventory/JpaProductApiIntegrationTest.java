package com.mateus.merchandiseinventory;

import com.mateus.merchandiseinventory.dao.factory.DaoFactory;
import com.mateus.merchandiseinventory.dao.factory.JpaDaoFactory;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.persistence.type=jpa")
class JpaProductApiIntegrationTest extends ProductApiIntegrationTest {

    @Override
    protected Class<? extends DaoFactory> expectedDaoFactory() {
        return JpaDaoFactory.class;
    }
}
