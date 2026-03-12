package org.poolpool.mohaeng.event.host.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.event.host.dto.EventCreateDto;
import org.poolpool.mohaeng.event.host.dto.HostEventMypageResponse;
import org.poolpool.mohaeng.event.host.dto.HostEventSummaryDto;
import org.poolpool.mohaeng.event.host.entity.HostBoothEntity;
import org.poolpool.mohaeng.event.host.entity.HostFacilityEntity;
import org.poolpool.mohaeng.event.host.repository.FileRepository;
import org.poolpool.mohaeng.event.host.repository.HostBoothRepository;
import org.poolpool.mohaeng.event.host.repository.HostFacilityRepository;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.poolpool.mohaeng.event.list.entity.EventCategoryEntity;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.entity.EventRegionEntity;
import org.poolpool.mohaeng.event.list.entity.FileEntity;
import org.poolpool.mohaeng.event.list.repository.EventCategoryRepository;
import org.poolpool.mohaeng.event.list.repository.EventRegionRepository;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.storage.s3.S3StorageService;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventHostServiceImpl implements EventHostService {

    private final EventRepository eventRepository;
    private final HostBoothRepository hostBoothRepository;
    private final HostFacilityRepository hostFacilityRepository;
    private final FileRepository fileRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final EventRegionRepository eventRegionRepository;
    private final S3StorageService s3StorageService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long createEventWithDetails(EventCreateDto createDto, Long hostId,
            MultipartFile thumbnail,
            List<MultipartFile> detailFiles,
            List<MultipartFile> boothFiles) {

        EventDto eventDto = createDto.getEventInfo();
        EventEntity eventEntity = eventDto.toEntity();

        // 카테고리
        if (eventDto.getCategory() != null && eventDto.getCategory().getCategoryId() != null) {
            EventCategoryEntity category = eventCategoryRepository.findById(eventDto.getCategory().getCategoryId())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다."));
            eventEntity.setCategory(category);
        } else {
            throw new RuntimeException("카테고리 정보가 누락되었습니다.");
        }

        // 지역
        if (eventDto.getRegion() != null && eventDto.getRegion().getRegionId() != null) {
            EventRegionEntity region = eventRegionRepository.findById(eventDto.getRegion().getRegionId())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 지역입니다."));
            eventEntity.setRegion(region);
        }

        // 주최자
        UserEntity host = userRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));
        eventEntity.setHost(host);

        // 실제 전달된 부스/시설 기준으로 override
        boolean actualHasBooth = createDto.getBooths() != null && !createDto.getBooths().isEmpty();
        boolean actualHasFacility = createDto.getFacilities() != null && !createDto.getFacilities().isEmpty();
        eventEntity.setHasBooth(actualHasBooth);
        eventEntity.setHasFacility(actualHasFacility);

        // 부스가 없으면 모집 기간 null 처리
        if (!actualHasBooth) {
            eventEntity.setBoothStartRecruit(null);
            eventEntity.setBoothEndRecruit(null);
        }

        // 썸네일 저장(S3)
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String savedName = s3StorageService.upload(thumbnail, "event");
                eventEntity.setThumbnail(savedName);
            } catch (Exception e) {
                throw new RuntimeException("썸네일 업로드 실패", e);
            }
        }

        // 임시 위험 점수 계산
        BigDecimal riskScore = calculateTemporaryRiskScore(eventEntity);
        BigDecimal thresholdScore = new BigDecimal("0.50");

        eventEntity.setAiRiskScore(riskScore);
        eventEntity.setAiCheckedAt(LocalDateTime.now());

        // 위험 점수 0.50 이상이면 관리자 검수
        if (riskScore.compareTo(thresholdScore) >= 0) {
            eventEntity.changeModerationStatusToPending();   // needsModeration = true, 승인대기
        } else {
            eventEntity.changeModerationStatusToApproved();  // needsModeration = false, 승인
        }

        // 행사 저장
        EventEntity savedEvent = eventRepository.save(eventEntity);
        Long eventId = savedEvent.getEventId();

        // 상세 이미지 저장(S3)
        if (detailFiles != null && !detailFiles.isEmpty()) {
            saveMultiFiles(detailFiles, savedEvent, "EVENT", "event");
        }

        // 부스 첨부파일 저장
        if (actualHasBooth && boothFiles != null && !boothFiles.isEmpty()) {
            saveMultiFiles(boothFiles, savedEvent, "HBOOTH", "host-booth");
        }

        // 부스 저장
        if (actualHasBooth) {
            List<HostBoothEntity> boothEntities = createDto.getBooths().stream()
                    .filter(dto -> dto.getBoothName() != null && !dto.getBoothName().isBlank())
                    .map(dto -> HostBoothEntity.builder()
                            .eventId(eventId)
                            .boothName(dto.getBoothName())
                            .boothPrice(dto.getBoothPrice())
                            .boothSize(dto.getBoothSize())
                            .boothNote(dto.getBoothNote())
                            .totalCount(dto.getTotalCount())
                            .remainCount(dto.getTotalCount())
                            .build())
                    .collect(Collectors.toList());
            if (!boothEntities.isEmpty()) {
                hostBoothRepository.saveAll(boothEntities);
            }
        }

        // 부대시설 저장
        if (actualHasFacility) {
            List<HostFacilityEntity> facilityEntities = createDto.getFacilities().stream()
                    .filter(dto -> dto.getFaciName() != null && !dto.getFaciName().isBlank())
                    .map(dto -> HostFacilityEntity.builder()
                            .eventId(eventId)
                            .faciName(dto.getFaciName())
                            .faciPrice(dto.getFaciPrice())
                            .faciUnit(dto.getFaciUnit())
                            .hasCount(dto.getHasCount())
                            .totalCount(dto.getHasCount() ? dto.getTotalCount() : null)
                            .remainCount(dto.getHasCount() ? dto.getTotalCount() : null)
                            .build())
                    .collect(Collectors.toList());
            if (!facilityEntities.isEmpty()) {
                hostFacilityRepository.saveAll(facilityEntities);
            }
        }

        return eventId;
    }

    private void saveMultiFiles(List<MultipartFile> files, EventEntity event, String fileType, String dir) {
        int sortOrder = 1;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            try {
                String savedName = s3StorageService.upload(file, dir);
                fileRepository.save(FileEntity.builder()
                        .event(event)
                        .fileType(fileType)
                        .originalFileName(original)
                        .renameFileName(savedName)
                        .sortOrder(sortOrder++)
                        .createdAt(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                throw new RuntimeException(fileType + " 다중 파일 업로드 실패", e);
            }
        }
    }

    private BigDecimal calculateTemporaryRiskScore(EventEntity eventEntity) {
        String text = buildModerationText(eventEntity).toLowerCase();
        BigDecimal score = BigDecimal.ZERO;

        // 1. 광고 / 스팸 / 도배
        if (containsAny(text,
                "무료", "할인", "특가", "선착순", "문의", "상담", "카톡", "오픈채팅", "링크", "홍보")) {
            score = score.add(new BigDecimal("0.30"));
        }

        // 2. 허위 정보 / 내용 불일치
        if (containsAny(text,
                "100% 보장", "무조건", "확정", "전액환불", "공식 확정", "검증완료")) {
            score = score.add(new BigDecimal("0.25"));
        }

        // 3. 도용 / 사칭 / 저작권 침해
        if (containsAny(text,
                "공식", "브랜드", "연예인", "저작권", "무단", "사칭", "제휴")) {
            score = score.add(new BigDecimal("0.35"));
        }

        // 4. 부적절한 내용
        if (containsAny(text,
                "도박", "불법", "성인", "음란", "마약", "혐오", "폭력")) {
            score = score.add(new BigDecimal("0.60"));
        }

        // 5. 중복 / 낚시성 등록
        if (containsAny(text,
                "긴급", "대박", "클릭", "실화", "충격", "마감임박", "지금바로")) {
            score = score.add(new BigDecimal("0.20"));
        }

        // 6. 기타
        if (text.contains("http://") || text.contains("https://") || text.contains("!!!")) {
            score = score.add(new BigDecimal("0.10"));
        }

        // 최대 1.00 제한
        if (score.compareTo(new BigDecimal("1.00")) > 0) {
            score = new BigDecimal("1.00");
        }

        return score;
    }

    private String buildModerationText(EventEntity eventEntity) {
        StringBuilder sb = new StringBuilder();

        if (eventEntity.getTitle() != null) {
            sb.append(eventEntity.getTitle()).append(" ");
        }
        if (eventEntity.getSimpleExplain() != null) {
            sb.append(eventEntity.getSimpleExplain()).append(" ");
        }
        if (eventEntity.getDescription() != null) {
            sb.append(eventEntity.getDescription()).append(" ");
        }

        return sb.toString().trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId, Long currentUserId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("해당 행사를 찾을 수 없습니다."));

        if (!event.getHost().getUserId().equals(currentUserId)) {
            throw new RuntimeException("본인이 생성한 행사만 삭제할 수 있습니다.");
        }

        if (!"행사종료".equals(event.getEventStatus())) {
            throw new RuntimeException("진행 중이거나 예정된 행사는 삭제할 수 없습니다. 종료 후 시도해주세요.");
        }

        event.changeStatusToDeleted();
    }

    @Override
    @Transactional(readOnly = true)
    public HostEventMypageResponse myEvents(Long hostId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EventEntity> p = eventRepository.findByHost_UserIdAndEventStatusNot(hostId, "DELETED", pageable);

        List<HostEventSummaryDto> items = p.getContent().stream()
                .map(HostEventSummaryDto::fromEntity)
                .toList();

        return HostEventMypageResponse.builder()
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveEvent(Long hostId) {
        return eventRepository.existsByHost_UserIdAndEventStatusNotIn(
                hostId,
                List.of("행사종료", "DELETED", "REPORT_DELETED", "report_deleted", "행사삭제")
        );
    }
}