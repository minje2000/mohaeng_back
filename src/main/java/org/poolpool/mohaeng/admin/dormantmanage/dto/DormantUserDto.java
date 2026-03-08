package org.poolpool.mohaeng.admin.dormantmanage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.poolpool.mohaeng.admin.dormantmanage.entity.DormantUserEntity;
import org.poolpool.mohaeng.admin.dormantmanage.type.DormantStatus;
import org.poolpool.mohaeng.user.entity.UserEntity;

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
public class DormantUserDto {

	private Long dormantId;
    private Long userId;

    private String email;              // 회원 테이블
    private LocalDate lastLoginAt;     // 회원 테이블

    private LocalDateTime notifiedAt;
    private LocalDateTime withdrawnAt;
    private DormantStatus dormantStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static DormantUserDto fromEntity(DormantUserEntity entity) {

        DormantUserDto dto = new DormantUserDto();

        dto.dormantId = entity.getDormantId();
        dto.userId = entity.getUser().getUserId();

        //UserEntity에서 가져오기
        dto.email = entity.getUser().getEmail();
        dto.lastLoginAt = entity.getUser().getLastLoginAt();

        dto.notifiedAt = entity.getNotifiedAt();
        dto.withdrawnAt = entity.getWithdrawnAt();
        dto.dormantStatus = entity.getDormantStatus();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();

        return dto;
    }
    
    public DormantUserEntity toEntity(UserEntity user) {

        DormantUserEntity entity = new DormantUserEntity();

        entity.setDormantId(this.dormantId);
        entity.setUser(user);  //UserEntity 전달
        entity.setNotifiedAt(this.notifiedAt);
        entity.setWithdrawnAt(this.withdrawnAt);
        entity.setDormantStatus(this.dormantStatus);
        entity.setCreatedAt(this.createdAt);
        entity.setUpdatedAt(this.updatedAt);

        return entity;
    }
}
