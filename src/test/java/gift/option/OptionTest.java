package gift.option;

import gift.TestFixtures;
import gift.product.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionTest {

    private final Product product = TestFixtures.product();

    @Test
    void constructor_zeroQuantity_throwsException() {
        assertThatThrownBy(() -> new Option(product, "옵션", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("수량은 1 이상");
    }

    @Test
    void constructor_negativeQuantity_throwsException() {
        assertThatThrownBy(() -> new Option(product, "옵션", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("수량은 1 이상");
    }

    @Test
    void constructor_validQuantity_createsSuccessfully() {
        var option = new Option(product, "옵션", 10);

        assertThat(option.getQuantity()).isEqualTo(10);
    }

    @Test
    void subtractQuantity_toZero_succeeds() {
        var option = new Option(product, "옵션", 5);

        option.subtractQuantity(5);

        assertThat(option.getQuantity()).isEqualTo(0);
    }
}
