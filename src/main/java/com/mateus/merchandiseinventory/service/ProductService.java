package com.mateus.merchandiseinventory.service;

import com.mateus.merchandiseinventory.dao.ProductDao;
import com.mateus.merchandiseinventory.dao.factory.DaoFactory;
import com.mateus.merchandiseinventory.dto.CreateProductRequest;
import com.mateus.merchandiseinventory.dto.ProductResponse;
import com.mateus.merchandiseinventory.dto.UpdateProductRequest;
import com.mateus.merchandiseinventory.entity.Product;
import com.mateus.merchandiseinventory.exception.ProductNotFoundException;
import com.mateus.merchandiseinventory.exception.ProductVersionConflictException;
import com.mateus.merchandiseinventory.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductDao productDao;
    private final ProductMapper productMapper;

    public ProductService(DaoFactory daoFactory, ProductMapper productMapper) {
        this.productDao = daoFactory.createProductDao();
        this.productMapper = productMapper;
        log.info("Persistência de produtos configurada com {}", productDao.getClass().getSimpleName());
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = productDao.save(productMapper.toEntity(request));
        log.info("Produto {} criado: nome='{}', preço={}, estoque={}",
                product.getId(), product.getName(), product.getPrice(), product.getStockQuantity());
        return productMapper.toResponse(product);
    }

    public ProductResponse findById(Long id) {
        log.debug("Consultando produto {}", id);
        return productMapper.toResponse(getProduct(id));
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        log.debug("Listando produtos: página={}, tamanho={}, ordenação={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return productDao.findAll(pageable).map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = getProduct(id);

        if (!product.getVersion().equals(request.version())) {
            throw new ProductVersionConflictException(id, request.version(), product.getVersion());
        }

        product.update(request.name(), request.description(), request.price(), request.stockQuantity());
        Product updated = productDao.update(product);
        log.info("Produto {} atualizado para a versão {}: nome='{}', preço={}, estoque={}",
                id, updated.getVersion(), updated.getName(), updated.getPrice(), updated.getStockQuantity());
        return productMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        productDao.delete(getProduct(id));
        log.info("Produto {} removido", id);
    }

    private Product getProduct(Long id) {
        return productDao.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
