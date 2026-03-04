package gift.product;

import gift.TestFixtures;
import gift.category.Category;
import gift.option.Option;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private final Category category = TestFixtures.category();

    @Test
    void constructor_zeroPrice_throwsException() {
        assertThatThrownBy(() -> new Product("상품", 0, "http://img.test/img.png", category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("가격은 1 이상");
    }

    @Test
    void constructor_negativePrice_throwsException() {
        assertThatThrownBy(() -> new Product("상품", -1, "http://img.test/img.png", category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("가격은 1 이상");
    }

    @Test
    void constructor_validPrice_createsSuccessfully() {
        var product = new Product("상품", 5000, "http://img.test/img.png", category);

        assertThat(product.getPrice()).isEqualTo(5000);
    }

    @Test
    void update_zeroPrice_throwsException() {
        var product = new Product("상품", 5000, "http://img.test/img.png", category);

        assertThatThrownBy(() -> product.update("수정됨", 0, "http://img.test/img.png", category))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("가격은 1 이상");
    }

    @Test
    void removeOption_lastOption_throwsException() {
        var product = new Product("상품", 5000, "http://img.test/img.png", category);
        var option = new Option(product, "기본 옵션", 10);
        product.getOptions().add(option);

        assertThatThrownBy(() -> product.removeOption(option))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("옵션이 1개인 상품");
    }

    @Test
    void removeOption_multipleOptions_removesSuccessfully() {
        var product = new Product("상품", 5000, "http://img.test/img.png", category);
        var option1 = new Option(product, "옵션1", 10);
        var option2 = new Option(product, "옵션2", 20);
        product.getOptions().add(option1);
        product.getOptions().add(option2);

        product.removeOption(option1);

        assertThat(product.getOptions()).hasSize(1);
        assertThat(product.getOptions().get(0).getName()).isEqualTo("옵션2");
    }
}
