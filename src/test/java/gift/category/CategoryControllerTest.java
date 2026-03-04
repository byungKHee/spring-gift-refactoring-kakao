package gift.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static gift.TestFixtures.category;
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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getCategories_returnsList() throws Exception {
        given(categoryService.findAll()).willReturn(List.of(category()));

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("교환권"));
    }

    @Test
    void createCategory_valid_returns201() throws Exception {
        given(categoryService.create(any(CategoryRequest.class))).willReturn(category());
        var body = new CategoryRequest("교환권", "#ffffff", "http://img.test/a.png", "설명");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("교환권"));
    }

    @Test
    void updateCategory_valid_returns200() throws Exception {
        given(categoryService.update(eq(1L), any(CategoryRequest.class))).willReturn(category());
        var body = new CategoryRequest("수정", "#000000", "http://img.test/b.png", "수정 설명");

        mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("교환권"));
    }

    @Test
    void updateCategory_notFound_returns404() throws Exception {
        given(categoryService.update(eq(99L), any(CategoryRequest.class)))
            .willThrow(new NoSuchElementException());
        var body = new CategoryRequest("없음", "#000000", "http://img.test/b.png", "설명");

        mockMvc.perform(put("/api/categories/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteCategory_returns204() throws Exception {
        willDoNothing().given(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1"))
            .andExpect(status().isNoContent());
    }
}
