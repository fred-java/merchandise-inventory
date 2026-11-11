package com.mateus.merchandiseinventory.dao;

import com.mateus.merchandiseinventory.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductDao {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Page<Product> findAll(Pageable pageable);

    Product update(Product product);

    void delete(Product product);
}
