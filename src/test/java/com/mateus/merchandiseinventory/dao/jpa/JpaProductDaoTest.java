package com.mateus.merchandiseinventory.dao.jpa;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.ProductDaoContractTest;
import com.mateus.merchandiseinventory.dao.factory.JpaDaoFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "app.persistence.type=jpa")
@Import(JpaDaoFactory.class)
class JpaProductDaoTest extends ProductDaoContractTest {

    @Autowired
    private EntityManager entityManager;

    @Override
    protected Class<? extends ProductDao> expectedImplementation() {
        return JpaProductDao.class;
    }

    @Override
    protected void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
