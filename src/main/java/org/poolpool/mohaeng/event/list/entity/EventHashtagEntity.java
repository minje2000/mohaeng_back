package org.poolpool.mohaeng.event.list.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_hashtag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EventHashtagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer hashtagId;

    @Column(nullable = false, length = 50)
    private String hashtagName;
}
