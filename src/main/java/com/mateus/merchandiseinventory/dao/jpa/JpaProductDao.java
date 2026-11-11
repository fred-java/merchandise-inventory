package com.mateus.merchandiseinventory.dao.jpa;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.ProductSortField;
import com.mateus.merchandiseinventory.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public class JpaProductDao implements ProductDao {

    private final EntityManager entityManager;

    public JpaProductDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Product save(Product product) {
        entityManager.persist(product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Product.class, id));
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        String orderBy = ProductSortField.orderByClause(pageable.getSort(), field -> "p." + field.property());

        List<Product> content = entityManager.createQuery("select p from Product p order by " + orderBy, Product.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery("select count(p) from Product p", Long.class)
                .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Product update(Product product) {
        try {
            Product merged = entityManager.merge(product);
            entityManager.flush();
            return merged;
        } catch (OptimisticLockException e) {
            throw new OptimisticLockingFailureException(conflictMessage(product), e);
        }
    }

    @Override
    public void delete(Product product) {
        try {
            entityManager.remove(entityManager.contains(product) ? product : entityManager.merge(product));
            entityManager.flush();
        } catch (OptimisticLockException e) {
            throw new OptimisticLockingFailureException(conflictMessage(product), e);
        }
    }

    private static String conflictMessage(Product product) {
        return "O produto %d foi alterado por outra transação".formatted(product.getId());
    }
}