package org.poolpool.mohaeng.event.list.dto;

import java.time.LocalDateTime;

import org.poolpool.mohaeng.event.list.entity.FileEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {
    private Long fileId;
    private Long eventId;
    private Long pctBoothId;
    private String fileType;
    private String originalFileName;
    private String renameFileName;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환
    public static FileDto fromEntity(FileEntity entity) {
        if (entity == null) return null;
        return FileDto.builder()
                .fileId(entity.getFileId())
                .eventId(entity.getEvent() != null ? entity.getEvent().getEventId() : null)
                .pctBoothId(entity.getPctBooth() != null ? entity.getPctBooth().getPctBoothId() : null)
                .fileType(entity.getFileType())
                .originalFileName(entity.getOriginalFileName())
                .renameFileName(entity.getRenameFileName())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // DTO -> Entity 변환 
    public FileEntity toEntity() {
        return FileEntity.builder()
                .fileId(this.fileId)
                .fileType(this.fileType)
                .originalFileName(this.originalFileName)
                .renameFileName(this.renameFileName)
                .sortOrder(this.sortOrder)
                .createdAt(this.createdAt)
                // 연관 관계인 Event 등은 보통 Service에서 ID로 조회 후 
                // .event(event) 식으로 따로 세팅해주는 경우가 많습니다.
                .build();
    }
}
