package com.mateus.merchandiseinventory.dao.factory;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.jpa.JpaProductDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.persistence.type", havingValue = "jpa", matchIfMissing = true)
public class JpaDaoFactory implements DaoFactory {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ProductDao createProductDao() {
        return new JpaProductDao(entityManager);
    }
}
