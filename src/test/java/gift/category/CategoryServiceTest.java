package gift.category;

import gift.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = TestFixtures.category(1L, "교환권");
    }

    @Test
    void findAll_returnsAllCategories() {
        var categories = List.of(category, TestFixtures.category(2L, "상품권"));
        given(categoryRepository.findAll()).willReturn(categories);

        var result = categoryService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("교환권");
    }

    @Test
    void create_savesAndReturns() {
        var request = new CategoryRequest("뷰티", "#ff0000", "http://img.test/beauty.png", "뷰티 카테고리");
        given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> invocation.getArgument(0));

        var result = categoryService.create(request);

        assertThat(result.getName()).isEqualTo("뷰티");
        then(categoryRepository).should().save(any(Category.class));
    }

    @Test
    void update_existingCategory_updatesFields() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        var request = new CategoryRequest("수정됨", "#000000", "http://img.test/updated.png", "수정 설명");

        var result = categoryService.update(1L, request);

        assertThat(result.getName()).isEqualTo("수정됨");
        assertThat(result.getColor()).isEqualTo("#000000");
    }

    @Test
    void update_nonExistent_throwsNoSuchElementException() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());
        var request = new CategoryRequest("수정됨", "#000000", "http://img.test/updated.png", "수정 설명");

        assertThatThrownBy(() -> categoryService.update(99L, request))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void delete_delegatesToRepository() {
        categoryService.delete(1L);

        then(categoryRepository).should().deleteById(1L);
    }
}
