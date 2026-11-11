package com.mateus.merchandiseinventory;

import com.mateus.merchandiseinventory.dao.factory.DaoFactory;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class ProductApiIntegrationTest {

    private static final String PRODUCTS = "/api/v1/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DaoFactory daoFactory;

    protected abstract Class<? extends DaoFactory> expectedDaoFactory();

    @Test
    void usesTheConfiguredDaoFactory() {
        assertThat(daoFactory).isInstanceOf(expectedDaoFactory());
    }

    @Test
    void createsAndFindsAProduct() throws Exception {
        long id = create(productJson("Farinha de Mandioca 1kg", "6.50", 30));

        mockMvc.perform(get(PRODUCTS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Farinha de Mandioca 1kg"))
                .andExpect(jsonPath("$.price").value(6.50))
                .andExpect(jsonPath("$.stockQuantity").value(30))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void updateWithAStaleVersionIsRejected() throws Exception {
        long id = create(productJson("Biscoito Recheado 130g", "3.29", 200));

        mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Biscoito Recheado 130g", "3.49", 180, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(3.49))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Biscoito Recheado 130g", "2.99", 220, 0)))
                .andExpect(status().isConflict());

        mockMvc.perform(get(PRODUCTS + "/" + id))
                .andExpect(jsonPath("$.price").value(3.49))
                .andExpect(jsonPath("$.stockQuantity").value(180));
    }

    @Test
    void listsProductsPageByPage() throws Exception {
        create(productJson("Cerveja Lata 350ml", "3.99", 500));
        create(productJson("Amaciante 2L", "15.90", 25));
        create(productJson("Batata Inglesa kg", "5.49", 80));

        mockMvc.perform(get(PRODUCTS).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.products.length()").value(2))
                .andExpect(jsonPath("$._embedded.products[0].name").value("Amaciante 2L"))
                .andExpect(jsonPath("$._embedded.products[1].name").value("Batata Inglesa kg"))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.prev").doesNotExist());

        mockMvc.perform(get(PRODUCTS).param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$._embedded.products.length()").value(1))
                .andExpect(jsonPath("$._embedded.products[0].name").value("Cerveja Lata 350ml"))
                .andExpect(jsonPath("$._links.prev.href").exists())
                .andExpect(jsonPath("$._links.next").doesNotExist());

        mockMvc.perform(get(PRODUCTS).param("sort", "description"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linksLeadFromTheRootToEachProduct() throws Exception {
        long id = create(productJson("Leite Integral 1L", "5.29", 48));

        String productsHref = JsonPath.read(mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1"))
                .andReturn().getResponse().getContentAsString(), "$._links.products.href");

        String productHref = JsonPath.read(mockMvc.perform(get(productsHref))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$._embedded.products[0]._links.self.href");

        mockMvc.perform(get(productHref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$._links.update.href").value(productHref))
                .andExpect(jsonPath("$._links.delete.href").value(productHref));
    }

    @Test
    void pageSizeIsCappedAtOneHundred() throws Exception {
        mockMvc.perform(get(PRODUCTS).param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void exposesTheOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/products']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/products/{id}']").exists());
    }

    @Test
    void deletedProductIsNoLongerFound() throws Exception {
        long id = create(productJson("Papel Toalha 2 rolos", "6.99", 40));

        mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
    }

    private long create(String body) throws Exception {
        MvcResult result = mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private static String productJson(String name, String price, int stock) {
        return """
                {"name": "%s", "price": %s, "stockQuantity": %d}
                """.formatted(name, price, stock);
    }

    private static String updateJson(String name, String price, int stock, long version) {
        return """
                {"name": "%s", "price": %s, "stockQuantity": %d, "version": %d}
                """.formatted(name, price, stock, version);
    }
}
