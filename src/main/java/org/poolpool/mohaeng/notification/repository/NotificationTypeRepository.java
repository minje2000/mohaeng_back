package org.poolpool.mohaeng.notification.repository;

import java.util.List;

import org.poolpool.mohaeng.notification.entity.NotificationTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeRepository extends JpaRepository<NotificationTypeEntity, Long> {

    List<NotificationTypeEntity> findAllByNotiTypeIdIn(List<Long> notiTypeIds);
}