package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.request.MerchantRegisterRequest;
import com.trading.dto.request.ProductCreateRequest;
import com.trading.dto.response.MerchantResponse;
import com.trading.dto.response.ProductResponse;
import com.trading.repository.MerchantRepository;
import com.trading.repository.ProductRepository;
import com.trading.service.MerchantService;
import com.trading.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductRepository productRepository;

    private MerchantResponse testMerchant;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        merchantRepository.deleteAll();

        // Create a test merchant
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Electronics Store")
                .username("productmerchant")
                .password("password123")
                .build();
        testMerchant = merchantService.register(merchantRequest);
    }

    @Test
    void createProduct_Success() throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .merchantId(testMerchant.getId())
                .name("iPhone 15 Pro")
                .description("Latest Apple smartphone with advanced features")
                .category("Electronics")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("iPhone 15 Pro"))
                .andExpect(jsonPath("$.data.category").value("Electronics"))
                .andExpect(jsonPath("$.data.merchantId").value(testMerchant.getId()));
    }

    @Test
    void createProduct_InvalidMerchant() throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .merchantId(99999L)
                .name("Test Product")
                .description("Test Description")
                .category("Electronics")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getProductById_Success() throws Exception {
        // Create a product first
        ProductCreateRequest request = ProductCreateRequest.builder()
                .merchantId(testMerchant.getId())
                .name("MacBook Pro")
                .description("Powerful laptop for professionals")
                .category("Computers")
                .build();
        ProductResponse product = productService.create(request);

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro"))
                .andExpect(jsonPath("$.data.category").value("Computers"));
    }

    @Test
    void getProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void searchProducts_ByKeyword() throws Exception {
        // Create multiple products
        createTestProduct("iPhone 15", "Latest Apple phone", "Electronics");
        createTestProduct("Samsung Galaxy", "Android smartphone", "Electronics");
        createTestProduct("MacBook Pro", "Apple laptop", "Computers");

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void searchProducts_ByCategory() throws Exception {
        // Create multiple products
        createTestProduct("iPhone 15", "Latest smartphone", "Electronics");
        createTestProduct("Samsung TV", "Smart TV", "Electronics");
        createTestProduct("MacBook Pro", "Laptop", "Computers");

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void searchProducts_ByKeywordAndCategory() throws Exception {
        // Create multiple products
        createTestProduct("iPhone 15", "Latest Apple phone", "Electronics");
        createTestProduct("MacBook Pro", "Apple laptop", "Computers");
        createTestProduct("Samsung Galaxy", "Android phone", "Electronics");

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "Apple")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15"));
    }

    @Test
    void searchProducts_AllProducts() throws Exception {
        // Create multiple products
        createTestProduct("Product 1", "Description 1", "Category1");
        createTestProduct("Product 2", "Description 2", "Category2");
        createTestProduct("Product 3", "Description 3", "Category3");

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(3)));
    }

    @Test
    void searchProducts_Pagination() throws Exception {
        // Create 15 products
        for (int i = 1; i <= 15; i++) {
            createTestProduct("Product " + i, "Description " + i, "Electronics");
        }

        // Get first page with size 5
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(5)))
                .andExpect(jsonPath("$.data.totalElements").value(15))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // Get second page
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(5)));
    }

    private ProductResponse createTestProduct(String name, String description, String category) {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .merchantId(testMerchant.getId())
                .name(name)
                .description(description)
                .category(category)
                .build();
        return productService.create(request);
    }
}
