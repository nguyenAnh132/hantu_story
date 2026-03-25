package com.hantu.post_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import com.hantu.post_service.dto.request.CreateStoryPartRequest;
import com.hantu.post_service.dto.request.UpdateStoryPartRequest;
import com.hantu.post_service.dto.response.StoryPartResponse;
import com.hantu.post_service.mapper.StoryPartMapper;
import com.hantu.post_service.entity.StoryPart;
import com.hantu.post_service.entity.Story;
import com.hantu.post_service.exception.AppException;
import com.hantu.post_service.exception.ErrorCode;
import com.hantu.post_service.repository.StoryPartRepository;
import com.hantu.post_service.repository.StoryRepository;
import com.hantu.post_service.StoryStatus;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryPartService {
    StoryPartRepository storyPartRepository;
    StoryRepository storyRepository;
    StoryPartMapper storyPartMapper;

    public StoryPartResponse createStoryPart(CreateStoryPartRequest request) {
        if (!storyRepository.existsById(request.getStoryId())) {
            throw new AppException(ErrorCode.STORY_NOT_FOUND);
        }
        if (storyPartRepository.existsByStoryIdAndOrder(request.getStoryId(), request.getOrder())) {
            throw new AppException(ErrorCode.STORY_PART_ORDER_CONFLICT);
        }

        var now = LocalDateTime.now();
        StoryPart part = new StoryPart();
        // Gán thủ công để tránh rủi ro mapper bỏ sót field khi build lệch cấu hình.
        part.setStoryId(request.getStoryId() == null ? null : request.getStoryId().trim());
        part.setOrder(request.getOrder());
        part.setContent(request.getContent() == null ? null : request.getContent().trim());
        part.setImageUrls(request.getImageUrls());
        part.setId(null);
        part.setCreatedAt(now);
        part.setUpdatedAt(now);
        return toStoryPartResponse(storyPartRepository.save(part));
    }

    public StoryPartResponse getStoryPartById(String storyPartId) {
        StoryPart part = findStoryPartById(storyPartId);
        Story story = findStoryById(part.getStoryId());
        validateStoryAccessible(story);
        return toStoryPartResponse(part);
    }

    public List<StoryPartResponse> getStoryPartsByStory(String storyId) {
        Story story = findStoryById(storyId);
        validateStoryAccessible(story);
        return storyPartRepository.findByStoryIdOrderByOrderAsc(storyId).stream()
                .map(this::toStoryPartResponse)
                .collect(Collectors.toList());
    }

    public StoryPartResponse updateStoryPart(String storyPartId, UpdateStoryPartRequest request) {
        StoryPart part = findStoryPartById(storyPartId);
        Story story = findStoryById(part.getStoryId());
        validateOwner(story);

        if (request.getOrder() != null
                && request.getOrder() != part.getOrder()
                && storyPartRepository.existsByStoryIdAndOrder(part.getStoryId(), request.getOrder())) {
            throw new AppException(ErrorCode.STORY_PART_ORDER_CONFLICT);
        }
        storyPartMapper.updateStoryPart(request, part);
        part.setUpdatedAt(LocalDateTime.now());
        return toStoryPartResponse(storyPartRepository.save(part));
    }

    public void deleteStoryPart(String storyPartId) {
        StoryPart part = findStoryPartById(storyPartId);
        Story story = findStoryById(part.getStoryId());
        validateOwner(story);
        storyPartRepository.delete(part);
    }

    private StoryPart findStoryPartById(String storyPartId) {
        return storyPartRepository.findById(storyPartId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_PART_NOT_FOUND));
    }

    private Story findStoryById(String storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
    }

    private void validateStoryAccessible(Story story) {
        if (StoryStatus.PUBLISHED.name().equals(story.getStatus())) {
            return;
        }

        // Draft/deleted stories are accessible only to owner.
        String currentUserId = currentUserIdOrNull();
        if (currentUserId == null || !currentUserId.equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.STORY_NOT_FOUND);
        }
    }

    private void validateOwner(Story story) {
        if (!currentUserId().equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private String currentUserIdOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private StoryPartResponse toStoryPartResponse(StoryPart part) {
        if (part == null) {
            return null;
        }

        return new StoryPartResponse(
                part.getId(),
                part.getStoryId(),
                part.getOrder(),
                part.getContent(),
                part.getImageUrls() == null ? Collections.emptyList() : part.getImageUrls(),
                part.getCreatedAt(),
                part.getUpdatedAt());
    }
}
