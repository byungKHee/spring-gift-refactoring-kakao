package gift.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static gift.TestFixtures.product;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void getProducts_returnsPagedProducts() throws Exception {
        var products = new PageImpl<>(List.of(product()));
        given(productService.findAll(any(Pageable.class))).willReturn(products);

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("테스트 상품"));
    }

    @Test
    void getProduct_existing_returns200() throws Exception {
        given(productService.findById(1L)).willReturn(product());

        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("테스트 상품"));
    }

    @Test
    void getProduct_notFound_returns404() throws Exception {
        given(productService.findById(99L)).willThrow(new NoSuchElementException());

        mockMvc.perform(get("/api/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createProduct_valid_returns201() throws Exception {
        given(productService.create(any(ProductRequest.class))).willReturn(product());
        var body = new ProductRequest("테스트 상품", 10000, "http://img.test/a.png", 1L);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("테스트 상품"));
    }

    @Test
    void createProduct_invalidName_returns400() throws Exception {
        var body = new ProductRequest("", 10000, "http://img.test/a.png", 1L);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateProduct_valid_returns200() throws Exception {
        given(productService.update(eq(1L), any(ProductRequest.class))).willReturn(product());
        var body = new ProductRequest("수정 상품", 20000, "http://img.test/b.png", 1L);

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("테스트 상품"));
    }

    @Test
    void deleteProduct_returns204() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/api/products/1"))
            .andExpect(status().isNoContent());
    }
}
