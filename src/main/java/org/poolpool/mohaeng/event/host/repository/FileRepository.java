package org.poolpool.mohaeng.event.host.repository;

import org.poolpool.mohaeng.event.list.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
}