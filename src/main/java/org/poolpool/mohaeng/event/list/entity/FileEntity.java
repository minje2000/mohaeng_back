package org.poolpool.mohaeng.event.list.entity;

import java.time.LocalDateTime;

import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pct_booth_id")
    private ParticipationBoothEntity pctBooth; 

    @Column(length = 50)
    private String fileType;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String renameFileName;

    private Integer sortOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
