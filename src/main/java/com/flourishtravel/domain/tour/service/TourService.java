package com.flourishtravel.domain.tour.service;

import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.tour.dto.ActivityRequest;
import com.flourishtravel.domain.tour.dto.AvailabilityCheckDto;
import com.flourishtravel.domain.tour.dto.ItineraryRequest;
import com.flourishtravel.domain.tour.dto.LocationRequest;
import com.flourishtravel.domain.tour.dto.TourDetailDto;
import com.flourishtravel.domain.tour.dto.TourRequest;
import com.flourishtravel.domain.tour.dto.TourSummaryDto;
import com.flourishtravel.domain.tour.dto.TourVideoRequest;
import com.flourishtravel.domain.tour.entity.Category;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourActivity;
import com.flourishtravel.domain.tour.entity.TourImage;
import com.flourishtravel.domain.tour.entity.TourItinerary;
import com.flourishtravel.domain.tour.entity.TourLocation;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.entity.TourVideo;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.booking.repository.SessionParticipantActivityAttendanceRepository;
import com.flourishtravel.domain.tour.repository.CategoryRepository;
import com.flourishtravel.domain.tour.repository.TourActivityRepository;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionActivityOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TourService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");

    private final TourRepository tourRepository;
    private final CategoryRepository categoryRepository;
    private final TourActivityRepository tourActivityRepository;
    private final TourSessionActivityOverrideRepository sessionActivityOverrideRepository;

    @Value("${app.flora.timezone:Asia/Ho_Chi_Minh}")
    private String tourTimezone;

    private LocalDate todayInTourZone() {
        return TourSessionStatusResolver.todayInZone(tourTimezone);
    }
    private final SessionParticipantActivityAttendanceRepository sessionParticipantActivityAttendanceRepository;

    @Transactional(readOnly = true)
    public Page<Tour> search(String destination, BigDecimal minPrice, BigDecimal maxPrice,
                             LocalDate startDate, UUID categoryId, Pageable pageable) {
        return tourRepository.search(
                destinationLikePattern(destination), minPrice, maxPrice, startDate, categoryId, pageable);
    }

    /**
     * Catalog công khai: cùng bộ lọc với {@link #search} (chỉ tour còn chỗ theo query native),
     * trả DTO đủ ảnh / danh mục / session cho trang "Tour trải nghiệm".
     */
    @Transactional(readOnly = true)
    public Page<TourSummaryDto> publicCatalog(String destination, BigDecimal minPrice, BigDecimal maxPrice,
                                              LocalDate startDate, UUID categoryId, Pageable pageable) {
        return search(destination, minPrice, maxPrice, startDate, categoryId, pageable).map(this::toSummary);
    }

    /** Danh sách catalog (không bắt buộc còn chỗ session) — trang Tour & Vé. */
    @Transactional(readOnly = true)
    public Page<TourSummaryDto> publicCatalogBrowse(String destination, BigDecimal minPrice, BigDecimal maxPrice,
                                                   UUID categoryId, String marketSegment, Pageable pageable) {
        return tourRepository.searchForSuggestion(
                        destinationLikePattern(destination), minPrice, maxPrice, categoryId,
                        normalizeMarketSegment(marketSegment), pageable)
                .map(this::toSummary);
    }

    private static String normalizeMarketSegment(String segment) {
        if (segment == null || segment.isBlank()) return null;
        return segment.trim().toLowerCase(Locale.ROOT);
    }

    /** Postgres: không truyền null vào LOWER(LIKE) — dùng %% (match all). */
    public static String destinationLikePattern(String destination) {
        if (destination == null || destination.isBlank()) {
            return "%%";
        }
        return "%" + destination.trim() + "%";
    }

    @Transactional(readOnly = true)
    public TourDetailDto getPublicDetail(UUID id) {
        return toDetail(getById(id), true);
    }

    @Transactional(readOnly = true)
    public TourDetailDto getPublicDetailBySlug(String slug) {
        return toDetail(getBySlug(slug), true);
    }

    @Transactional(readOnly = true)
    public List<TourSummaryDto> getSimilarSummaries(UUID tourId, int limit) {
        return getSimilarTours(tourId, limit).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Tour getById(UUID id) {
        return tourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", id));
    }

    @Transactional(readOnly = true)
    public Tour getBySlug(String slug) {
        return tourRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", slug));
    }

    /** Tour tương tự: cùng category hoặc mới nhất, trừ tour hiện tại (AI-FEATURES: "Có thể bạn cũng thích"). */
    @Transactional(readOnly = true)
    public List<Tour> getSimilarTours(UUID tourId, int limit) {
        Tour tour = getById(tourId);
        Pageable page = PageRequest.of(0, limit);
        if (tour.getCategory() != null && tour.getCategory().getId() != null) {
            Page<Tour> byCategory = tourRepository.findByCategory_IdAndIdNotOrderByCreatedAtDesc(tour.getCategory().getId(), tourId, page);
            if (!byCategory.isEmpty()) return byCategory.getContent();
        }
        return tourRepository.findByIdNotOrderByCreatedAtDesc(tourId, page).getContent();
    }

    /** Kiểm tra còn chỗ: tìm session sớm nhất còn slot theo địa điểm (cho chatbot / API). */
    @Transactional(readOnly = true)
    public Optional<AvailabilityCheckDto> checkAvailability(String destination, LocalDate fromDate) {
        Page<Tour> tours = tourRepository.search(
                destinationLikePattern(destination),
                null, null, fromDate != null ? fromDate : LocalDate.now(), null,
                PageRequest.of(0, 10));
        for (Tour t : tours.getContent()) {
            if (t.getSessions() == null) continue;
            for (TourSession s : t.getSessions()) {
                if (!"scheduled".equals(s.getStatus())) continue;
                if (s.getCurrentParticipants() == null || s.getMaxParticipants() == null) continue;
                if (s.getCurrentParticipants() < s.getMaxParticipants()) {
                    int remaining = s.getMaxParticipants() - s.getCurrentParticipants();
                    return Optional.of(AvailabilityCheckDto.builder()
                            .remainingSlots(remaining)
                            .nextStartDate(s.getStartDate())
                            .tourTitle(t.getTitle())
                            .tourId(t.getId())
                            .sessionId(s.getId())
                            .build());
                }
            }
        }
        return Optional.empty();
    }

    // ---------- Admin operations ----------

    /** Danh sách tour cho admin (không lọc session) — trả về DTO tóm tắt. */
    @Transactional(readOnly = true)
    public Page<TourSummaryDto> adminList(String q, String status, Pageable pageable) {
        // Truyền pattern không-null để tránh lỗi Postgres "lower(bytea) does not exist".
        String term = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + term + "%";
        Page<Tour> page = tourRepository.adminSearch(pattern, pageable);
        Page<TourSummaryDto> mapped = page.map(this::toSummary);
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return mapped;
        }
        List<TourSummaryDto> filtered = mapped.getContent().stream()
                .filter(s -> status.equalsIgnoreCase(s.getStatus()))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
    }

    /** Chi tiết tour cho admin (full quan hệ). */
    @Transactional(readOnly = true)
    public TourDetailDto getAdminDetail(UUID id) {
        Tour tour = getById(id);
        return toDetail(tour, false);
    }

    @Transactional
    public Tour create(TourRequest req) {
        String slug = resolveSlug(req.getSlug(), req.getTitle());
        ensureSlugAvailable(slug, null);

        Tour tour = new Tour();
        tour.setTitle(req.getTitle().trim());
        tour.setSlug(slug);
        tour.setDescription(trimToNull(req.getDescription()));
        tour.setBasePrice(req.getBasePrice());
        tour.setDurationDays(req.getDurationDays());
        tour.setDurationNights(req.getDurationNights());
        tour.setCategory(resolveCategory(req.getCategoryId()));
        tour.setDestinationCity(trimToNull(req.getDestinationCity()));
        tour.setMarketSegment(normalizeMarketSegment(req.getMarketSegment()));

        applyImages(tour, req);
        applyVideos(tour, req);
        return tourRepository.save(tour);
    }

    @Transactional
    public Tour update(UUID id, TourRequest req) {
        Tour tour = getById(id);
        String slug = resolveSlug(req.getSlug(), req.getTitle());
        ensureSlugAvailable(slug, id);

        tour.setTitle(req.getTitle().trim());
        tour.setSlug(slug);
        tour.setDescription(trimToNull(req.getDescription()));
        tour.setBasePrice(req.getBasePrice());
        tour.setDurationDays(req.getDurationDays());
        tour.setDurationNights(req.getDurationNights());
        tour.setCategory(resolveCategory(req.getCategoryId()));
        tour.setDestinationCity(trimToNull(req.getDestinationCity()));
        tour.setMarketSegment(normalizeMarketSegment(req.getMarketSegment()));

        if (req.getImageUrls() != null) {
            tour.getImages().clear();
            applyImages(tour, req);
        }
        if (req.getVideos() != null) {
            tour.getVideos().clear();
            applyVideos(tour, req);
        }
        return tourRepository.save(tour);
    }

    private void applyImages(Tour tour, TourRequest req) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (req.getImageUrls() != null) {
            for (String raw : req.getImageUrls()) {
                String u = trimToMax(raw, 500);
                if (u != null) {
                    urls.add(u);
                }
            }
        }
        if (urls.isEmpty() && req.getThumbnailUrl() != null && !req.getThumbnailUrl().isBlank()) {
            String u = trimToMax(req.getThumbnailUrl(), 500);
            if (u != null) {
                urls.add(u);
            }
        }
        int order = 0;
        for (String url : urls) {
            tour.getImages().add(TourImage.builder()
                    .tour(tour)
                    .imageUrl(url)
                    .caption(order == 0 ? tour.getTitle() : null)
                    .sortOrder(order++)
                    .build());
        }
    }

    private void applyVideos(Tour tour, TourRequest req) {
        if (req.getVideos() == null) {
            return;
        }
        int order = 0;
        for (TourVideoRequest v : req.getVideos()) {
            if (v == null || v.getVideoUrl() == null || v.getVideoUrl().isBlank()) {
                continue;
            }
            tour.getVideos().add(TourVideo.builder()
                    .tour(tour)
                    .videoUrl(trimToMax(v.getVideoUrl(), 500))
                    .thumbnailUrl(trimToMax(v.getThumbnailUrl(), 500))
                    .title(trimToNull(v.getTitle()))
                    .durationSeconds(v.getDurationSeconds())
                    .sortOrder(order++)
                    .build());
        }
    }

    @Transactional
    public void delete(UUID id) {
        Tour tour = getById(id);
        try {
            tourRepository.delete(tour);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BadRequestException("Không thể xoá tour vì đang có booking liên quan");
        }
    }

    // ---------- Helpers ----------

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private void ensureSlugAvailable(String slug, UUID currentId) {
        Optional<Tour> existing = tourRepository.findBySlug(slug);
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

    /** Trim chuỗi; nếu dài hơn maxLen thì cắt bớt (tránh lỗi DB khi URL S3 quá dài). */
    private static String trimToMax(String value, int maxLen) {
        if (value == null) return null;
        String t = value.trim();
        if (t.isEmpty()) return null;
        if (t.length() <= maxLen) return t;
        return t.substring(0, maxLen);
    }

    private TourSummaryDto toSummary(Tour tour) {
        TourSummaryDto.CategoryRef catRef = null;
        if (tour.getCategory() != null) {
            Category c = tour.getCategory();
            catRef = TourSummaryDto.CategoryRef.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .archived(c.getDeletedAt() != null)
                    .build();
        }

        String thumb = null;
        if (tour.getImages() != null && !tour.getImages().isEmpty()) {
            thumb = tour.getImages().stream()
                    .sorted(Comparator.comparing(
                            TourImage::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .findFirst()
                    .map(TourImage::getImageUrl)
                    .orElse(null);
        }

        TourSummaryDto.SessionRef earliest = null;
        int sessionsCount = 0;
        String status = "draft";
        if (tour.getSessions() != null && !tour.getSessions().isEmpty()) {
            sessionsCount = tour.getSessions().size();
            Optional<TourSession> earliestOpt = tour.getSessions().stream()
                    .filter(s -> s.getStartDate() != null)
                    .min(Comparator.comparing(TourSession::getStartDate));
            if (earliestOpt.isPresent()) {
                TourSession s = earliestOpt.get();
                LocalDate today = todayInTourZone();
                earliest = TourSummaryDto.SessionRef.builder()
                        .id(s.getId())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .maxParticipants(s.getMaxParticipants())
                        .currentParticipants(s.getCurrentParticipants())
                        .status(TourSessionStatusResolver.resolveEffectiveStatus(s, today))
                        .build();
                status = computeStatus(tour.getSessions(), today);
            }
        }

        return TourSummaryDto.builder()
                .id(tour.getId())
                .title(tour.getTitle())
                .slug(tour.getSlug())
                .description(tour.getDescription())
                .basePrice(tour.getBasePrice())
                .durationDays(tour.getDurationDays())
                .durationNights(tour.getDurationNights())
                .destinationCity(tour.getDestinationCity())
                .marketSegment(tour.getMarketSegment())
                .thumbnailUrl(thumb)
                .category(catRef)
                .earliestSession(earliest)
                .sessionsCount(sessionsCount)
                .status(status)
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                .build();
    }

    private TourDetailDto toDetail(Tour tour, boolean redactGuideContact) {
        TourSummaryDto.CategoryRef catRef = null;
        if (tour.getCategory() != null) {
            Category c = tour.getCategory();
            catRef = TourSummaryDto.CategoryRef.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .archived(c.getDeletedAt() != null)
                    .build();
        }

        List<TourDetailDto.ImageRef> images = tour.getImages() == null ? List.of() :
                tour.getImages().stream()
                        .sorted(Comparator.comparing(
                                TourImage::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(img -> TourDetailDto.ImageRef.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .caption(img.getCaption())
                                .sortOrder(img.getSortOrder())
                                .build())
                        .toList();

        List<TourDetailDto.VideoRef> videos = tour.getVideos() == null ? List.of() :
                tour.getVideos().stream()
                        .sorted(Comparator.comparing(
                                TourVideo::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(v -> TourDetailDto.VideoRef.builder()
                                .id(v.getId())
                                .videoUrl(v.getVideoUrl())
                                .thumbnailUrl(v.getThumbnailUrl())
                                .title(v.getTitle())
                                .durationSeconds(v.getDurationSeconds())
                                .sortOrder(v.getSortOrder())
                                .build())
                        .toList();

        List<TourDetailDto.ItineraryRef> itineraries = tour.getItineraries() == null ? List.of() :
                tour.getItineraries().stream()
                        .sorted(Comparator.comparing(
                                TourItinerary::getDayNumber,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toItineraryRef)
                        .toList();

        List<TourDetailDto.LocationRef> locations = tour.getLocations() == null ? List.of() :
                tour.getLocations().stream()
                        .sorted(Comparator.comparing(
                                TourLocation::getVisitOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(loc -> TourDetailDto.LocationRef.builder()
                                .id(loc.getId())
                                .locationName(loc.getLocationName())
                                .latitude(loc.getLatitude())
                                .longitude(loc.getLongitude())
                                .visitOrder(loc.getVisitOrder())
                                .dayNumber(loc.getDayNumber())
                                .build())
                        .toList();

        List<TourDetailDto.SessionDetail> sessions = tour.getSessions() == null ? List.of() :
                tour.getSessions().stream()
                        .sorted(Comparator.comparing(
                                TourSession::getStartDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(s -> {
                            TourDetailDto.GuideRef guideRef = null;
                            User guide = s.getTourGuide();
                            if (guide != null) {
                                TourDetailDto.GuideRef.GuideRefBuilder gb = TourDetailDto.GuideRef.builder()
                                        .id(guide.getId())
                                        .fullName(guide.getFullName())
                                        .avatarUrl(guide.getAvatarUrl());
                                if (!redactGuideContact) {
                                    gb.email(guide.getEmail());
                                }
                                guideRef = gb.build();
                            }
                            LocalDate today = todayInTourZone();
                            return TourDetailDto.SessionDetail.builder()
                                    .id(s.getId())
                                    .startDate(s.getStartDate())
                                    .endDate(s.getEndDate())
                                    .maxParticipants(s.getMaxParticipants())
                                    .currentParticipants(s.getCurrentParticipants())
                                    .status(TourSessionStatusResolver.resolveEffectiveStatus(s, today))
                                    .tourGuide(guideRef)
                                    .build();
                        })
                        .toList();

        String status = (tour.getSessions() == null || tour.getSessions().isEmpty())
                ? "draft"
                : computeStatus(tour.getSessions(), todayInTourZone());

        String thumbnailUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        return TourDetailDto.builder()
                .id(tour.getId())
                .title(tour.getTitle())
                .slug(tour.getSlug())
                .description(tour.getDescription())
                .basePrice(tour.getBasePrice())
                .durationDays(tour.getDurationDays())
                .durationNights(tour.getDurationNights())
                .category(catRef)
                .status(status)
                .images(images)
                .videos(videos)
                .itineraries(itineraries)
                .locations(locations)
                .sessions(sessions)
                .destinationCity(tour.getDestinationCity())
                .rating(tour.getRating())
                .badge(tour.getBadge())
                .tags(splitCsv(tour.getTags()))
                .highlights(splitLines(tour.getHighlightsText()))
                .includes(splitLines(tour.getIncludesText()))
                .excludes(splitLines(tour.getExcludesText()))
                .thumbnailUrl(thumbnailUrl)
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                .build();
    }

    private List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> splitLines(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return java.util.Arrays.stream(raw.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private TourDetailDto.ItineraryRef toItineraryRef(TourItinerary it) {
        List<TourDetailDto.ActivityRef> activityRefs = it.getActivities() == null ? List.of() :
                it.getActivities().stream()
                        .sorted(Comparator.comparing(
                                TourActivity::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(a -> TourDetailDto.ActivityRef.builder()
                                .id(a.getId())
                                .sortOrder(a.getSortOrder())
                                .startTime(a.getStartTime())
                                .endTime(a.getEndTime())
                                .durationMinutes(a.getDurationMinutes())
                                .title(a.getTitle())
                                .description(a.getDescription())
                                .activityType(a.getActivityType())
                                .locationName(a.getLocationName())
                                .latitude(a.getLatitude())
                                .longitude(a.getLongitude())
                                .imageUrl(a.getImageUrl())
                                .costEstimate(a.getCostEstimate())
                                .costIncluded(a.getCostIncluded())
                                .tags(a.getTags())
                                .locationAddress(a.getLocationAddress())
                                .isGatheringEvent(a.getIsGatheringEvent())
                                .gatheringEventType(a.getGatheringEventType())
                                .scheduleStatus(a.getScheduleStatus())
                                .build())
                        .toList();

        return TourDetailDto.ItineraryRef.builder()
                .id(it.getId())
                .dayNumber(it.getDayNumber())
                .title(it.getTitle())
                .description(it.getDescription())
                .summary(it.getSummary())
                .coverImageUrl(it.getCoverImageUrl())
                .accommodation(it.getAccommodation())
                .transport(it.getTransport())
                .mealsIncluded(it.getMealsIncluded())
                .highlights(it.getHighlights())
                .activities(activityRefs)
                .build();
    }

    // ---------- Itinerary admin operations ----------

    /** Lấy danh sách lịch trình đầy đủ cho admin (cho trang Itinerary Builder). */
    @Transactional(readOnly = true)
    public List<TourDetailDto.ItineraryRef> getItinerary(UUID tourId) {
        Tour tour = getById(tourId);
        if (tour.getItineraries() == null || tour.getItineraries().isEmpty()) return List.of();
        return tour.getItineraries().stream()
                .sorted(Comparator.comparing(
                        TourItinerary::getDayNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toItineraryRef)
                .toList();
    }

    /**
     * Bulk replace toàn bộ itineraries + activities của tour theo payload từ Itinerary Builder.
     * Vì cascade ALL + orphanRemoval đã set ở Tour và Itinerary nên clear() + add() sẽ tự xoá các bản ghi cũ.
     */
    @Transactional
    public List<TourDetailDto.ItineraryRef> saveItinerary(UUID tourId, List<ItineraryRequest> days) {
        if (!tourRepository.existsById(tourId)) {
            throw new ResourceNotFoundException("Tour", tourId);
        }

        // Xoá attendance + overrides + activities + itineraries (native SQL, tránh FK / stale persistence context).
        sessionParticipantActivityAttendanceRepository.deleteByTourActivityTourId(tourId);
        sessionActivityOverrideRepository.deleteByTourActivityTourId(tourId);
        tourActivityRepository.deleteByTourId(tourId);
        tourRepository.deleteItinerariesByTourId(tourId);

        Tour tour = tourRepository.findByIdWithItinerariesAndActivities(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", tourId));
        if (tour.getItineraries() == null) {
            tour.setItineraries(new java.util.ArrayList<>());
        } else {
            tour.getItineraries().clear();
        }

        if (days != null) {
            for (ItineraryRequest d : days) {
                TourItinerary it = TourItinerary.builder()
                        .tour(tour)
                        .dayNumber(d.getDayNumber())
                        .title(safeTrim(d.getTitle()))
                        .description(trimToNull(d.getDescription()))
                        .summary(trimToNull(d.getSummary()))
                        .coverImageUrl(trimToMax(d.getCoverImageUrl(), 2000))
                        .accommodation(trimToNull(d.getAccommodation()))
                        .transport(trimToNull(d.getTransport()))
                        .mealsIncluded(trimToNull(d.getMealsIncluded()))
                        .highlights(trimToNull(d.getHighlights()))
                        .activities(new java.util.ArrayList<>())
                        .build();

                if (d.getActivities() != null) {
                    int idx = 0;
                    for (ActivityRequest a : d.getActivities()) {
                        TourActivity act = TourActivity.builder()
                                .itinerary(it)
                                .sortOrder(a.getSortOrder() != null ? a.getSortOrder() : idx)
                                .startTime(a.getStartTime())
                                .endTime(a.getEndTime())
                                .durationMinutes(a.getDurationMinutes())
                                .title(trimToNull(a.getTitle()))
                                .description(trimToNull(a.getDescription()))
                                .activityType(trimToNull(a.getActivityType()))
                                .locationName(trimToNull(a.getLocationName()))
                                .latitude(roundCoordinate(a.getLatitude()))
                                .longitude(roundCoordinate(a.getLongitude()))
                                .imageUrl(trimToMax(a.getImageUrl(), 2000))
                                .costEstimate(a.getCostEstimate())
                                .costIncluded(a.getCostIncluded() != null ? a.getCostIncluded() : Boolean.TRUE)
                                .tags(trimToMax(a.getTags(), 500))
                                .locationAddress(trimToMax(a.getLocationAddress(), 2000))
                                .isGatheringEvent(Boolean.TRUE.equals(a.getIsGatheringEvent()))
                                .gatheringEventType(trimToNull(a.getGatheringEventType()))
                                .scheduleStatus(normalizeScheduleStatus(a.getScheduleStatus()))
                                .build();
                        validateActivityTimes(act);
                        it.getActivities().add(act);
                        idx++;
                    }
                }

                tour.getItineraries().add(it);
            }
        }
        tourRepository.saveAndFlush(tour);

        Tour reloaded = tourRepository.findByIdWithItinerariesAndActivities(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", tourId));
        if (reloaded.getItineraries() == null || reloaded.getItineraries().isEmpty()) {
            return List.of();
        }
        return reloaded.getItineraries().stream()
                .sorted(Comparator.comparing(
                        TourItinerary::getDayNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toItineraryRef)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TourDetailDto.LocationRef> getLocations(UUID tourId) {
        Tour tour = getById(tourId);
        return toLocationRefs(tour);
    }

    /** Bulk replace toàn bộ địa điểm tour (theo ngày lịch trình). */
    @Transactional
    public List<TourDetailDto.LocationRef> saveLocations(UUID tourId, List<LocationRequest> items) {
        Tour tour = getById(tourId);
        if (tour.getLocations() == null) {
            tour.setLocations(new java.util.ArrayList<>());
        } else {
            tour.getLocations().clear();
        }
        tourRepository.saveAndFlush(tour);

        if (items != null) {
            int fallbackOrder = 0;
            for (LocationRequest req : items) {
                String name = trimToNull(req.getLocationName());
                if (name == null) {
                    continue;
                }
                TourLocation loc = TourLocation.builder()
                        .tour(tour)
                        .dayNumber(req.getDayNumber() != null ? req.getDayNumber() : 1)
                        .visitOrder(req.getVisitOrder() != null ? req.getVisitOrder() : fallbackOrder++)
                        .locationName(name)
                        .latitude(roundCoordinate(req.getLatitude()))
                        .longitude(roundCoordinate(req.getLongitude()))
                        .build();
                validateLocationCoordinates(loc);
                tour.getLocations().add(loc);
            }
        }
        Tour saved = tourRepository.save(tour);
        return toLocationRefs(saved);
    }

    private List<TourDetailDto.LocationRef> toLocationRefs(Tour tour) {
        if (tour.getLocations() == null || tour.getLocations().isEmpty()) {
            return List.of();
        }
        return tour.getLocations().stream()
                .sorted(Comparator
                        .comparing(TourLocation::getDayNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TourLocation::getVisitOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(loc -> TourDetailDto.LocationRef.builder()
                        .id(loc.getId())
                        .locationName(loc.getLocationName())
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .visitOrder(loc.getVisitOrder())
                        .dayNumber(loc.getDayNumber())
                        .build())
                .toList();
    }

    private static void validateLocationCoordinates(TourLocation loc) {
        boolean hasLat = loc.getLatitude() != null;
        boolean hasLon = loc.getLongitude() != null;
        if (hasLat != hasLon) {
            throw new BadRequestException("latitude và longitude phải được nhập cùng nhau cho địa điểm"
                    + (loc.getLocationName() != null ? " (" + loc.getLocationName() + ")" : ""));
        }
        if (hasLat) {
            double lat = loc.getLatitude().doubleValue();
            double lon = loc.getLongitude().doubleValue();
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new BadRequestException("Tọa độ GPS không hợp lệ cho địa điểm"
                        + (loc.getLocationName() != null ? " (" + loc.getLocationName() + ")" : ""));
            }
        }
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static BigDecimal roundCoordinate(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(7, RoundingMode.HALF_UP);
    }

    private static String normalizeScheduleStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().toUpperCase();
    }

    private static void validateActivityTimes(TourActivity act) {
        if (act.getStartTime() != null && act.getEndTime() != null && !act.getEndTime().isAfter(act.getStartTime())) {
            throw new BadRequestException("Giờ kết thúc hoạt động phải sau giờ bắt đầu" +
                    (act.getTitle() != null ? " (" + act.getTitle() + ")" : ""));
        }
        boolean hasLat = act.getLatitude() != null;
        boolean hasLon = act.getLongitude() != null;
        if (hasLat != hasLon) {
            throw new BadRequestException("latitude và longitude phải được nhập cùng nhau cho hoạt động"
                    + (act.getTitle() != null ? " (" + act.getTitle() + ")" : ""));
        }
        if (hasLat) {
            double lat = act.getLatitude().doubleValue();
            double lon = act.getLongitude().doubleValue();
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new BadRequestException("Tọa độ GPS không hợp lệ cho hoạt động"
                        + (act.getTitle() != null ? " (" + act.getTitle() + ")" : ""));
            }
        }
    }

    /**
     * Suy luận status hiển thị cho admin / public:
     *   - ongoing  : có session đang diễn ra (start ≤ hôm nay ≤ end)
     *   - completed: mọi session đã kết thúc hoặc huỷ (không còn scheduled/ongoing)
     *   - full     : session scheduled tương lai đều hết chỗ
     *   - upcoming : tất cả scheduled còn chỗ và start_date &gt; today + 30 ngày
     *   - active   : còn session scheduled có thể đặt
     *   - draft    : không có session
     */
    private String computeStatus(List<TourSession> sessions, LocalDate today) {
        if (sessions == null || sessions.isEmpty()) {
            return "draft";
        }

        List<String> effective = sessions.stream()
                .map(s -> TourSessionStatusResolver.resolveEffectiveStatus(s, today))
                .toList();

        if (effective.stream().anyMatch(TourSessionStatusResolver.ONGOING::equals)) {
            return "ongoing";
        }

        boolean anyBookableFuture = sessions.stream().anyMatch(s -> {
            String eff = TourSessionStatusResolver.resolveEffectiveStatus(s, today);
            return TourSessionStatusResolver.SCHEDULED.equals(eff);
        });
        if (!anyBookableFuture) {
            boolean anyCompleted = effective.stream().anyMatch(TourSessionStatusResolver.COMPLETED::equals);
            return anyCompleted ? "completed" : "draft";
        }

        List<TourSession> scheduledFuture = sessions.stream()
                .filter(s -> TourSessionStatusResolver.SCHEDULED.equals(
                        TourSessionStatusResolver.resolveEffectiveStatus(s, today)))
                .toList();

        boolean allFull = scheduledFuture.stream().allMatch(s ->
                s.getMaxParticipants() != null
                && s.getCurrentParticipants() != null
                && s.getCurrentParticipants() >= s.getMaxParticipants());
        if (allFull && !scheduledFuture.isEmpty()) {
            return "full";
        }

        boolean allUpcoming = scheduledFuture.stream().allMatch(s ->
                s.getStartDate() != null && s.getStartDate().isAfter(today.plusDays(30)));
        if (allUpcoming && !scheduledFuture.isEmpty()) {
            return "upcoming";
        }

        return "active";
    }
}
