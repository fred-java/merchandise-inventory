package com.mateus.merchandiseinventory.dao.jdbc;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.ProductSortField;
import com.mateus.merchandiseinventory.entity.Product;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class JdbcProductDao implements ProductDao {

    private static final String COLUMNS =
            "id, name, description, price, stock_quantity, version, created_at, updated_at";

    private static final RowMapper<Product> ROW_MAPPER = (rs, rowNum) -> Product.restore(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getInt("stock_quantity"),
            rs.getLong("version"),
            rs.getObject("created_at", Instant.class),
            rs.getObject("updated_at", Instant.class)
    );

    private final JdbcClient jdbcClient;

    public JdbcProductDao(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Product save(Product product) {
        Instant now = now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql("""
                        insert into products (name, description, price, stock_quantity, version, created_at, updated_at)
                        values (:name, :description, :price, :stockQuantity, 0, :now, :now)
                        """)
                .param("name", product.getName())
                .param("description", product.getDescription())
                .param("price", product.getPrice())
                .param("stockQuantity", product.getStockQuantity())
                .param("now", now)
                .update(keyHolder, "id");

        return Product.restore(keyHolder.getKeyAs(Long.class), product.getName(), product.getDescription(),
                product.getPrice(), product.getStockQuantity(), 0L, now, now);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jdbcClient.sql("select " + COLUMNS + " from products where id = :id")
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        String orderBy = ProductSortField.orderByClause(pageable.getSort(), ProductSortField::column);

        List<Product> content = jdbcClient.sql("select " + COLUMNS + " from products order by " + orderBy
                        + " limit :limit offset :offset")
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query(ROW_MAPPER)
                .list();

        Long total = jdbcClient.sql("select count(*) from products")
                .query(Long.class)
                .single();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Product update(Product product) {
        Instant now = now();

        int updatedRows = jdbcClient.sql("""
                        update products
                           set name = :name,
                               description = :description,
                               price = :price,
                               stock_quantity = :stockQuantity,
                               version = version + 1,
                               updated_at = :updatedAt
                         where id = :id
                           and version = :version
                        """)
                .param("name", product.getName())
                .param("description", product.getDescription())
                .param("price", product.getPrice())
                .param("stockQuantity", product.getStockQuantity())
                .param("updatedAt", now)
                .param("id", product.getId())
                .param("version", product.getVersion())
                .update();

        if (updatedRows == 0) {
            throw new OptimisticLockingFailureException(conflictMessage(product));
        }

        return Product.restore(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getStockQuantity(), product.getVersion() + 1, product.getCreatedAt(), now);
    }

    @Override
    public void delete(Product product) {
        int deletedRows = jdbcClient.sql("delete from products where id = :id and version = :version")
                .param("id", product.getId())
                .param("version", product.getVersion())
                .update();

        if (deletedRows == 0) {
            throw new OptimisticLockingFailureException(conflictMessage(product));
        }
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static String conflictMessage(Product product) {
        return "O produto %d foi alterado por outra transação".formatted(product.getId());
    }
}
