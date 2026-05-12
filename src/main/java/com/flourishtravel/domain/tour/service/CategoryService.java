package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.dto.CategoryRequest;
import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");

    private final CategoryRepository categoryRepository;

    /** Danh mục đang hoạt động (mặc định cho UI public + admin). */
    @Transactional(readOnly = true)
    public List<Category> listActive() {
        return categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc();
    }

    /** Danh mục đã lưu trữ (admin only). */
    @Transactional(readOnly = true)
    public List<Category> listArchived() {
        return categoryRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();
    }

    /** Lấy theo id, chỉ khi đang hoạt động — dùng cho update. */
    @Transactional(readOnly = true)
    public Category getActiveById(UUID id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    /** Lấy bất kỳ (active hoặc archived) — dùng cho restore. */
    @Transactional(readOnly = true)
    public Category getAnyById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional
    public Category create(CategoryRequest req) {
        String slug = resolveSlug(req.getSlug(), req.getName());
        ensureSlugAvailable(slug, null);

        Category category = Category.builder()
                .name(req.getName().trim())
                .slug(slug)
                .description(trimToNull(req.getDescription()))
                .sortOrder(req.getSortOrder())
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(UUID id, CategoryRequest req) {
        Category category = getActiveById(id);

        String slug = resolveSlug(req.getSlug(), req.getName());
        ensureSlugAvailable(slug, id);

        category.setName(req.getName().trim());
        category.setSlug(slug);
        category.setDescription(trimToNull(req.getDescription()));
        category.setSortOrder(req.getSortOrder());
        return categoryRepository.save(category);
    }

    /**
     * Soft-delete: chỉ đánh dấu deletedAt, giữ FK với tour không vỡ và có thể khôi phục.
     */
    @Transactional
    public void softDelete(UUID id) {
        Category category = getActiveById(id);
        category.setDeletedAt(Instant.now());
        categoryRepository.save(category);
    }

    /** Khôi phục danh mục đã lưu trữ. */
    @Transactional
    public Category restore(UUID id) {
        Category category = getAnyById(id);
        if (category.getDeletedAt() == null) {
            throw new BadRequestException("Danh mục đang ở trạng thái hoạt động, không cần khôi phục");
        }
        // Nếu trong khi đang lưu trữ, có category khác đang dùng cùng slug → từ chối khôi phục
        ensureSlugAvailable(category.getSlug(), id);
        category.setDeletedAt(null);
        return categoryRepository.save(category);
    }

    /** Slug chỉ được phép trùng với chính bản ghi hiện tại trong số các record đang hoạt động. */
    private void ensureSlugAvailable(String slug, UUID currentId) {
        Optional<Category> existing = categoryRepository.findBySlugAndDeletedAtIsNull(slug);
        if (existing.isPresent() && (currentId == null || !existing.get().getId().equals(currentId))) {
            throw new BadRequestException("Slug '" + slug + "' đã tồn tại");
        }
    }

    private String resolveSlug(String rawSlug, String name) {
        if (rawSlug != null && !rawSlug.isBlank()) {
            return rawSlug.trim().toLowerCase(Locale.ROOT);
        }
        return toSlug(name);
    }

    /** Chuyển tên có dấu sang slug an toàn cho URL. */
    private String toSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'd');
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = EDGE_DASHES.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
