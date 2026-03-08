package org.poolpool.mohaeng.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTypeEntity {

    @Id
    @Column(name = "NOTITYPE_ID")
    private Long notiTypeId;

    @Column(name = "NOTITYPE_NAME", nullable = false, length = 50)
    private String notiTypeName;

    @Column(name = "NOTITYPE_CONTENTS", nullable = false, columnDefinition = "TEXT")
    private String notiTypeContents;
}