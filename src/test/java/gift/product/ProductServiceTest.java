package gift.product;

import gift.TestFixtures;
import gift.category.Category;
import gift.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = TestFixtures.category(1L, "교환권");
        product = TestFixtures.product(1L, "테스트 상품", category);
    }

    @Test
    void findAll_returnsPage() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(product));
        given(productRepository.findAll(pageable)).willReturn(page);

        var result = productService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findById_existing_returns() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        var result = productService.findById(1L);

        assertThat(result.getName()).isEqualTo("테스트 상품");
    }

    @Test
    void findById_nonExistent_throws() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_validRequest_saves() {
        var request = new ProductRequest("새 상품", 5000, "http://img.test/new.png", 1L);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        var result = productService.create(request);

        assertThat(result.getName()).isEqualTo("새 상품");
        then(productRepository).should().save(any(Product.class));
    }

    @Test
    void create_invalidName_throws() {
        var request = new ProductRequest("", 5000, "http://img.test/new.png", 1L);

        assertThatThrownBy(() -> productService.create(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_categoryNotFound_throws() {
        var request = new ProductRequest("새 상품", 5000, "http://img.test/new.png", 99L);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_kakaoName_allowFalse_throws() {
        var request = new ProductRequest("카카오상품", 5000, "http://img.test/new.png", 1L);

        assertThatThrownBy(() -> productService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("카카오");
    }

    @Test
    void update_happyPath_updates() {
        var request = new ProductRequest("수정됨", 9999, "http://img.test/updated.png", 1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        var result = productService.update(1L, request);

        assertThat(result.getName()).isEqualTo("수정됨");
        assertThat(result.getPrice()).isEqualTo(9999);
    }

    @Test
    void update_notFound_throws() {
        var request = new ProductRequest("수정됨", 9999, "http://img.test/updated.png", 1L);
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, request))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void delete_delegatesToRepository() {
        productService.delete(1L);

        then(productRepository).should().deleteById(1L);
    }
}
