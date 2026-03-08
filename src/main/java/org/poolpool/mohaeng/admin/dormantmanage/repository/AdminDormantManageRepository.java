package org.poolpool.mohaeng.admin.dormantmanage.repository;

import java.util.List;

import org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto;
import org.poolpool.mohaeng.admin.dormantmanage.entity.DormantUserEntity;
import org.poolpool.mohaeng.admin.dormantmanage.type.DormantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

public interface AdminDormantManageRepository extends JpaRepository<DormantUserEntity, Long> {

	//휴면 계정 관리 프로시저 호출
	@Procedure(procedureName = "dormant_user_proc")
    void callDormantUserProc();
	
	//휴면 계정 관리 조회
    @Query("""
        select new org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto(
            d.dormantId,
            u.userId,
            u.email,
            u.lastLoginAt,
            d.notifiedAt,
            d.withdrawnAt,
            d.dormantStatus,
            d.createdAt,
            d.updatedAt
        )
        from DormantUserEntity d
        join d.user u
        where d.dormantStatus not in ('CANCELED','WITHDRAWN')
    """)
    Page<DormantUserDto> findDormantUsers(Pageable pageable);
    
    //안내 메일 보낼 휴면 계정 조회, 탈퇴 처리할 휴면 계정 조회
    @Query("""
        select new org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto(
            d.dormantId,
            u.userId,
            u.email,
            u.lastLoginAt,
            d.notifiedAt,
            d.withdrawnAt,
            d.dormantStatus,
            d.createdAt,
            d.updatedAt
        )
        from DormantUserEntity d
        join d.user u
        where d.dormantStatus = :status
    """)
    List<DormantUserDto> findDormantUsersByDormantStatus(@Param("status") DormantStatus status);
    
    
}
