package com.flourishtravel.domain.tour.repository;

import com.flourishtravel.domain.tour.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderBySortOrderAsc();

    /** Chỉ các danh mục đang hoạt động (chưa soft-delete). */
    List<Category> findAllByDeletedAtIsNullOrderBySortOrderAsc();

    /** Chỉ các danh mục đã lưu trữ. */
    List<Category> findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();

    /** Tìm theo slug trong số đang hoạt động (dùng cho check trùng). */
    Optional<Category> findBySlugAndDeletedAtIsNull(String slug);

    /** Lấy theo id chỉ khi đang hoạt động. */
    Optional<Category> findByIdAndDeletedAtIsNull(UUID id);
}
