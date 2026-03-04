package gift.option;

import gift.TestFixtures;
import gift.product.Product;
import gift.product.ProductRepository;
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
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionService optionService;

    private Product product;
    private Option option;

    @BeforeEach
    void setUp() {
        product = TestFixtures.product();
        option = TestFixtures.option(1L, product, "기본 옵션", 100);
    }

    @Test
    void findByProductId_exists_returnsOptions() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findByProductId(1L)).willReturn(List.of(option));

        var result = optionService.findByProductId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("기본 옵션");
    }

    @Test
    void findByProductId_productNotFound_throws() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionService.findByProductId(99L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_happyPath_saves() {
        var request = new OptionRequest("새 옵션", 50);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.existsByProductIdAndName(1L, "새 옵션")).willReturn(false);
        given(optionRepository.save(any(Option.class))).willAnswer(inv -> inv.getArgument(0));

        var result = optionService.create(1L, request);

        assertThat(result.getName()).isEqualTo("새 옵션");
        then(optionRepository).should().save(any(Option.class));
    }

    @Test
    void create_invalidName_throws() {
        var request = new OptionRequest("", 50);

        assertThatThrownBy(() -> optionService.create(1L, request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_productNotFound_throws() {
        var request = new OptionRequest("새 옵션", 50);
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionService.create(99L, request))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_duplicateName_throws() {
        var request = new OptionRequest("기본 옵션", 50);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.existsByProductIdAndName(1L, "기본 옵션")).willReturn(true);

        assertThatThrownBy(() -> optionService.create(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 존재하는 옵션명");
    }

    @Test
    void delete_happyPath_deletes() {
        var option2 = TestFixtures.option(2L, product, "추가 옵션", 50);
        product.getOptions().add(option);
        product.getOptions().add(option2);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        optionService.delete(1L, 1L);

        assertThat(product.getOptions()).doesNotContain(option);
    }

    @Test
    void delete_onlyOneOption_throws() {
        product.getOptions().add(option);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findById(1L)).willReturn(Optional.of(option));

        assertThatThrownBy(() -> optionService.delete(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("옵션이 1개인 상품");
    }

    @Test
    void delete_optionNotOwnedByProduct_throws() {
        var otherProduct = TestFixtures.product(2L, "다른 상품", TestFixtures.category());
        var foreignOption = TestFixtures.option(3L, otherProduct, "외부 옵션", 10);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(optionRepository.findById(3L)).willReturn(Optional.of(foreignOption));

        assertThatThrownBy(() -> optionService.delete(1L, 3L))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void delete_productNotFound_throws() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> optionService.delete(99L, 1L))
            .isInstanceOf(NoSuchElementException.class);
    }
}
