package gift.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category create(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow();
        category.update(request.name(), request.color(), request.imageUrl(), request.description());
        return category;
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
