package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Product create(ProductRequest request) {
        return create(request, false);
    }

    @Transactional
    public Product create(ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Category category = categoryRepository.findById(request.categoryId()).orElseThrow();
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        return update(id, request, false);
    }

    @Transactional
    public Product update(Long id, ProductRequest request, boolean allowKakao) {
        validateName(request.name(), allowKakao);
        Product product = productRepository.findById(id).orElseThrow();
        Category category = categoryRepository.findById(request.categoryId()).orElseThrow();
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
