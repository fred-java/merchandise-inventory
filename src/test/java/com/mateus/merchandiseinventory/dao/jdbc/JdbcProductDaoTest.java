package com.mateus.merchandiseinventory.dao.jdbc;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.ProductDaoContractTest;
import com.mateus.merchandiseinventory.dao.factory.JdbcDaoFactory;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

@JdbcTest(properties = "app.persistence.type=jdbc")
@Import(JdbcDaoFactory.class)
class JdbcProductDaoTest extends ProductDaoContractTest {

    @Override
    protected Class<? extends ProductDao> expectedImplementation() {
        return JdbcProductDao.class;
    }
}
