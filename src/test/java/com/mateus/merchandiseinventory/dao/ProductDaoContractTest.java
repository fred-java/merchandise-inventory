package com.mateus.merchandiseinventory.dao;

import com.mateus.merchandiseinventory.dao.factory.DaoFactory;
import com.mateus.merchandiseinventory.entity.Product;
import com.mateus.merchandiseinventory.exception.InvalidSortPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public abstract class ProductDaoContractTest {

    @Autowired
    private DaoFactory daoFactory;

    @Autowired
    private DataSource dataSource;

    protected ProductDao productDao;

    private JdbcClient jdbcClient;

    protected abstract Class<? extends ProductDao> expectedImplementation();

    protected void clearPersistenceContext() {
    }

    @BeforeEach
    void setUp() {
        productDao = daoFactory.createProductDao();
        jdbcClient = JdbcClient.create(dataSource);
    }

    @Test
    void factoryCreatesTheExpectedImplementation() {
        assertThat(productDao).isInstanceOf(expectedImplementation());
    }

    @Test
    void savesAndFindsAProduct() {
        Product saved = productDao.save(new Product("Leite Integral 1L", "Caixa", new BigDecimal("5.29"), 48));
        clearPersistenceContext();

        Product found = productDao.findById(saved.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Leite Integral 1L");
        assertThat(found.getDescription()).isEqualTo("Caixa");
        assertThat(found.getPrice()).isEqualByComparingTo("5.29");
        assertThat(found.getStockQuantity()).isEqualTo(48);
        assertThat(found.getVersion()).isZero();
        assertThat(found.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(found.getUpdatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void returnsEmptyWhenTheIdDoesNotExist() {
        assertThat(productDao.findById(12345L)).isEmpty();
    }

    @Test
    void paginatesAndSorts() {
        productDao.save(product("Banana Prata kg", "6.99", 30));
        productDao.save(product("Acucar Refinado 1kg", "4.79", 10));
        productDao.save(product("Macarrao Espaguete 500g", "3.89", 50));
        productDao.save(product("Detergente 500ml", "2.49", 20));
        productDao.save(product("Cafe Torrado 500g", "19.90", 40));

        Page<Product> firstPage = productDao.findAll(PageRequest.of(0, 2, Sort.by("price").descending()));
        Page<Product> lastPage = productDao.findAll(PageRequest.of(2, 2, Sort.by("price").descending()));

        assertThat(firstPage.getContent()).extracting(Product::getName)
                .containsExactly("Cafe Torrado 500g", "Banana Prata kg");
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(lastPage.getContent()).extracting(Product::getName).containsExactly("Detergente 500ml");
    }

    @Test
    void sortsByPropertiesWhoseColumnHasAnotherName() {
        productDao.save(product("Banana Prata kg", "6.99", 30));
        productDao.save(product("Acucar Refinado 1kg", "4.79", 10));
        productDao.save(product("Detergente 500ml", "2.49", 20));

        Page<Product> page = productDao.findAll(PageRequest.of(0, 10, Sort.by("stockQuantity")));

        assertThat(page.getContent()).extracting(Product::getStockQuantity).containsExactly(10, 20, 30);
    }

    @Test
    void rejectsUnknownSortProperties() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("name; drop table products"));

        assertThatThrownBy(() -> productDao.findAll(pageable))
                .isInstanceOf(InvalidSortPropertyException.class);
    }

    @Test
    void updateSavesTheChangesAndIncrementsTheVersion() {
        Product product = productDao.save(product("Sabonete 90g", "1.99", 100));

        product.update("Sabonete 90g", "Erva doce", new BigDecimal("2.19"), 90);
        Product updated = productDao.update(product);
        clearPersistenceContext();

        Product found = productDao.findById(product.getId()).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(found.getVersion()).isEqualTo(1L);
        assertThat(found.getDescription()).isEqualTo("Erva doce");
        assertThat(found.getPrice()).isEqualByComparingTo("2.19");
        assertThat(found.getStockQuantity()).isEqualTo(90);
    }

    @Test
    void updateFailsWhenAnotherTransactionChangedTheRow() {
        Product product = productDao.save(product("Sabonete 90g", "1.99", 100));
        bumpVersionInTheDatabase(product.getId());

        product.update("Sabonete 90g", null, new BigDecimal("2.50"), 80);

        assertThatThrownBy(() -> productDao.update(product))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void deletesAProduct() {
        Product product = productDao.save(product("Agua Mineral 500ml", "1.50", 200));

        productDao.delete(product);
        clearPersistenceContext();

        assertThat(productDao.findById(product.getId())).isEmpty();
    }

    @Test
    void deleteFailsWhenAnotherTransactionChangedTheRow() {
        Product product = productDao.save(product("Agua Mineral 500ml", "1.50", 200));
        bumpVersionInTheDatabase(product.getId());

        assertThatThrownBy(() -> productDao.delete(product))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    private void bumpVersionInTheDatabase(Long id) {
        jdbcClient.sql("update products set version = version + 1 where id = :id")
                .param("id", id)
                .update();
    }

    private static Product product(String name, String price, int stock) {
        return new Product(name, null, new BigDecimal(price), stock);
    }
}
