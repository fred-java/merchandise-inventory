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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private DaoFactory daoFactory;

    @Mock
    private ProductDao productDao;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        when(daoFactory.createProductDao()).thenReturn(productDao);
        productService = new ProductService(daoFactory, new ProductMapper());
    }

    @Nested
    class Create {

        @Test
        void savesTheProductAndReturnsIt() {
            when(productDao.save(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                ReflectionTestUtils.setField(product, "id", 1L);
                ReflectionTestUtils.setField(product, "version", 0L);
                return product;
            });

            ProductResponse response = productService.create(
                    new CreateProductRequest("Feijao Carioca 1kg", "Tipo 1", new BigDecimal("8.99"), 40));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productDao).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Feijao Carioca 1kg");
            assertThat(captor.getValue().getStockQuantity()).isEqualTo(40);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.price()).isEqualByComparingTo("8.99");
            assertThat(response.version()).isZero();
        }
    }

    @Nested
    class FindById {

        @Test
        void returnsTheProductWhenItExists() {
            when(productDao.findById(1L)).thenReturn(Optional.of(product(1L, 0L)));

            ProductResponse response = productService.findById(1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Cafe Torrado 500g");
        }

        @Test
        void throwsWhenTheProductDoesNotExist() {
            when(productDao.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    class FindAll {

        @Test
        void mapsThePageReturnedByTheDao() {
            Pageable pageable = PageRequest.of(1, 2);
            when(productDao.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(product(3L, 0L), product(4L, 2L)), pageable, 5));

            Page<ProductResponse> page = productService.findAll(pageable);

            assertThat(page.getContent()).extracting(ProductResponse::id).containsExactly(3L, 4L);
            assertThat(page.getNumber()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    class Update {

        private final UpdateProductRequest request =
                new UpdateProductRequest("Cafe Torrado 1kg", "Extra forte", new BigDecimal("39.90"), 12, 3L);

        @Test
        void updatesWhenTheVersionMatches() {
            Product product = product(1L, 3L);
            when(productDao.findById(1L)).thenReturn(Optional.of(product));
            when(productDao.update(product)).thenAnswer(invocation -> {
                ReflectionTestUtils.setField(product, "version", 4L);
                return product;
            });

            ProductResponse response = productService.update(1L, request);

            assertThat(response.name()).isEqualTo("Cafe Torrado 1kg");
            assertThat(response.description()).isEqualTo("Extra forte");
            assertThat(response.price()).isEqualByComparingTo("39.90");
            assertThat(response.stockQuantity()).isEqualTo(12);
            assertThat(response.version()).isEqualTo(4L);
        }

        @Test
        void rejectsAStaleVersion() {
            Product product = product(1L, 5L);
            when(productDao.findById(1L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.update(1L, request))
                    .isInstanceOf(ProductVersionConflictException.class);

            verify(productDao, never()).update(any());
            assertThat(product.getName()).isEqualTo("Cafe Torrado 500g");
        }

        @Test
        void throwsWhenTheProductDoesNotExist() {
            when(productDao.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(1L, request))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesAnExistingProduct() {
            Product product = product(1L, 0L);
            when(productDao.findById(1L)).thenReturn(Optional.of(product));

            productService.delete(1L);

            verify(productDao).delete(product);
        }

        @Test
        void throwsWhenTheProductDoesNotExist() {
            when(productDao.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.delete(1L))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(productDao, never()).delete(any());
        }
    }

    private static Product product(Long id, Long version) {
        Product product = new Product("Cafe Torrado 500g", "Tradicional", new BigDecimal("19.90"), 30);
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "version", version);
        return product;
    }
}
