package com.trading.property;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Product module
 * Feature: trading-system
 */
@SpringBootTest
@ActiveProfiles("test")
public class ProductPropertyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    /**
     * Property 14: Search Result Relevance
     * For any product search with keyword K, all returned products SHALL contain K 
     * in either the product name or description.
     * Validates: Requirements 3.2
     */
    @Test
    void searchResultsShouldContainKeywordInNameOrDescription() {
        Random random = new Random();
        String[] keywords = {"phone", "laptop", "camera", "tablet", "watch", "speaker", "headphone", "monitor", "keyboard", "mouse"};
        String[] categories = {"Electronics", "Accessories", "Computers", "Audio", "Wearables"};

        // Create a merchant
        String uniqueUsername = "merchant_" + UUID.randomUUID().toString().substring(0, 8);
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Electronics Store")
                .username(uniqueUsername)
                .password("password123")
                .build();
        MerchantResponse merchant = merchantService.register(merchantRequest);

        // Create products with various names and descriptions
        for (int i = 0; i < 50; i++) {
            String keyword = keywords[random.nextInt(keywords.length)];
            String category = categories[random.nextInt(categories.length)];
            
            // Randomly put keyword in name or description
            String name;
            String description;
            if (random.nextBoolean()) {
                name = "Premium " + keyword + " Model " + i;
                description = "High quality product with great features";
            } else {
                name = "Product Model " + i;
                description = "This is a great " + keyword + " with amazing features";
            }

            ProductCreateRequest productRequest = ProductCreateRequest.builder()
                    .merchantId(merchant.getId())
                    .name(name)
                    .description(description)
                    .category(category)
                    .build();
            productService.create(productRequest);
        }

        // Also create some products without any of the keywords
        for (int i = 0; i < 10; i++) {
            ProductCreateRequest productRequest = ProductCreateRequest.builder()
                    .merchantId(merchant.getId())
                    .name("Generic Item " + i)
                    .description("A simple product without special keywords")
                    .category("Other")
                    .build();
            productService.create(productRequest);
        }

        // Test search for each keyword
        for (int iteration = 0; iteration < 100; iteration++) {
            String searchKeyword = keywords[random.nextInt(keywords.length)];
            
            Page<ProductResponse> results = productService.search(searchKeyword, null, PageRequest.of(0, 100));

            // Property: all returned products must contain the keyword in name or description
            for (ProductResponse product : results.getContent()) {
                String nameLower = product.getName().toLowerCase();
                String descLower = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
                String keywordLower = searchKeyword.toLowerCase();

                boolean containsKeyword = nameLower.contains(keywordLower) || descLower.contains(keywordLower);
                
                assertThat(containsKeyword)
                        .as("Product '%s' with description '%s' should contain keyword '%s' in name or description",
                                product.getName(), product.getDescription(), searchKeyword)
                        .isTrue();
            }
        }
    }

    /**
     * Additional test: Search with category filter should return products in that category
     */
    @Test
    void searchWithCategoryFilterShouldReturnProductsInCategory() {
        Random random = new Random();
        String[] categories = {"Electronics", "Accessories", "Computers", "Audio", "Wearables"};

        // Create a merchant
        String uniqueUsername = "merchant_" + UUID.randomUUID().toString().substring(0, 8);
        MerchantRegisterRequest merchantRequest = MerchantRegisterRequest.builder()
                .businessName("Test Store")
                .username(uniqueUsername)
                .password("password123")
                .build();
        MerchantResponse merchant = merchantService.register(merchantRequest);

        // Create products in different categories
        for (int i = 0; i < 50; i++) {
            String category = categories[random.nextInt(categories.length)];
            
            ProductCreateRequest productRequest = ProductCreateRequest.builder()
                    .merchantId(merchant.getId())
                    .name("Product " + i)
                    .description("Description for product " + i)
                    .category(category)
                    .build();
            productService.create(productRequest);
        }

        // Test search for each category
        for (int iteration = 0; iteration < 100; iteration++) {
            String searchCategory = categories[random.nextInt(categories.length)];
            
            Page<ProductResponse> results = productService.search(null, searchCategory, PageRequest.of(0, 100));

            // Property: all returned products must be in the specified category
            for (ProductResponse product : results.getContent()) {
                assertThat(product.getCategory())
                        .as("Product '%s' should be in category '%s'", product.getName(), searchCategory)
                        .isEqualTo(searchCategory);
            }
        }
    }
}
