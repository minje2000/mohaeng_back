package org.poolpool.mohaeng.admin.report.entity;

import java.time.LocalDateTime;

import org.poolpool.mohaeng.admin.report.type.ReportResult;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "event_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_report_reporter_event",
        columnNames = {"reporter_id", "event_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportFEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false, length = 50)
    private String reasonCategory;

    @Column(columnDefinition = "TEXT")
    private String reasonDetailText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String reportResult;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.reportResult == null) this.reportResult = ReportResult.PENDING;
    }
}