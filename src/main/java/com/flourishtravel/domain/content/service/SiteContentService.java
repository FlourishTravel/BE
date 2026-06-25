package com.flourishtravel.domain.content.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.content.dto.CreateSiteContentRequest;
import com.flourishtravel.domain.content.dto.SiteContentDto;
import com.flourishtravel.domain.content.dto.UpdateSiteContentRequest;
import com.flourishtravel.domain.content.entity.SiteContent;
import com.flourishtravel.domain.content.repository.SiteContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SiteContentService {

    private static final Set<String> ALLOWED_TYPES = Set.of("news", "story", "career", "help");

    private final SiteContentRepository siteContentRepository;

    @Transactional(readOnly = true)
    public List<SiteContentDto> listPublic(String type) {
        return siteContentRepository.findPublic(normalizeType(type)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteContentDto getPublicBySlug(String slug) {
        SiteContent content = siteContentRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("SiteContent", slug));
        return toDto(content);
    }

    @Transactional(readOnly = true)
    public List<SiteContentDto> listAdmin(String type) {
        return siteContentRepository.findAdmin(normalizeType(type)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteContentDto getAdmin(UUID id) {
        SiteContent content = siteContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiteContent", id));
        return toDto(content);
    }

    @Transactional
    public SiteContentDto create(CreateSiteContentRequest request) {
        String slug = normalizeRequired(request.getSlug(), "slug");
        if (siteContentRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("slug đã tồn tại");
        }
        SiteContent content = SiteContent.builder()
                .type(requiredType(request.getType()))
                .slug(slug)
                .title(normalizeRequired(request.getTitle(), "title"))
                .summary(normalizeNullable(request.getSummary()))
                .body(normalizeNullable(request.getBody()))
                .imageUrl(normalizeNullable(request.getImageUrl()))
                .category(normalizeNullable(request.getCategory()))
                .published(Boolean.TRUE.equals(request.getPublished()))
                .sortOrder(request.getSortOrder())
                .publishedAt(resolvePublishedAt(request.getPublishedAt(), request.getPublished()))
                .build();
        return toDto(siteContentRepository.save(content));
    }

    @Transactional
    public SiteContentDto update(UUID id, UpdateSiteContentRequest request) {
        SiteContent content = siteContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiteContent", id));

        if (request.getType() != null) content.setType(requiredType(request.getType()));
        if (request.getSlug() != null) {
            String slug = normalizeRequired(request.getSlug(), "slug");
            if (!content.getSlug().equalsIgnoreCase(slug) && siteContentRepository.existsBySlugIgnoreCase(slug)) {
                throw new BadRequestException("slug đã tồn tại");
            }
            content.setSlug(slug);
        }
        if (request.getTitle() != null) content.setTitle(normalizeRequired(request.getTitle(), "title"));
        if (request.getSummary() != null) content.setSummary(normalizeNullable(request.getSummary()));
        if (request.getBody() != null) content.setBody(normalizeNullable(request.getBody()));
        if (request.getImageUrl() != null) content.setImageUrl(normalizeNullable(request.getImageUrl()));
        if (request.getCategory() != null) content.setCategory(normalizeNullable(request.getCategory()));
        if (request.getSortOrder() != null) content.setSortOrder(request.getSortOrder());
        if (request.getPublished() != null) {
            content.setPublished(request.getPublished());
            if (Boolean.TRUE.equals(request.getPublished()) && content.getPublishedAt() == null) {
                content.setPublishedAt(Instant.now());
            }
            if (Boolean.FALSE.equals(request.getPublished())) {
                content.setPublishedAt(null);
            }
        }
        if (request.getPublishedAt() != null) content.setPublishedAt(request.getPublishedAt());
        return toDto(siteContentRepository.save(content));
    }

    @Transactional
    public void delete(UUID id) {
        SiteContent content = siteContentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiteContent", id));
        siteContentRepository.delete(content);
    }

    private String normalizeNullable(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String field) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new BadRequestException(field + " không được để trống");
        }
        return normalized;
    }

    private String normalizeType(String type) {
        String normalized = normalizeNullable(type);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String requiredType(String type) {
        String normalized = normalizeType(type);
        if (normalized == null || !ALLOWED_TYPES.contains(normalized)) {
            throw new BadRequestException("type chỉ hỗ trợ news|story|career|help");
        }
        return normalized;
    }

    private Instant resolvePublishedAt(Instant publishedAt, Boolean published) {
        if (publishedAt != null) return publishedAt;
        if (Boolean.TRUE.equals(published)) return Instant.now();
        return null;
    }

    private SiteContentDto toDto(SiteContent content) {
        return SiteContentDto.builder()
                .id(content.getId())
                .type(content.getType())
                .slug(content.getSlug())
                .title(content.getTitle())
                .summary(content.getSummary())
                .body(content.getBody())
                .imageUrl(content.getImageUrl())
                .category(content.getCategory())
                .published(content.getPublished())
                .sortOrder(content.getSortOrder())
                .publishedAt(content.getPublishedAt())
                .build();
    }
}
