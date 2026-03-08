package org.poolpool.mohaeng.admin.dormantmanage.service;

import java.time.LocalDate;
import java.util.List;

import org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto;
import org.poolpool.mohaeng.admin.dormantmanage.type.DormantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminDormantManageService {

	//휴면 계정 관리 프로시저 호출
	void callDormantUserProc();

	//휴면 계정 관리 조회
	Page<DormantUserDto> findDormantUsers(Pageable pageable);

	//안내 메일 보낼 휴면 계정 조회, 탈퇴 처리할 휴면 계정 조회
	List<DormantUserDto> findDormantUsersByDormantStatus(DormantStatus status);
	
	//안내 메일 전송
	void sendDormantMail(String email, LocalDate lastLoginDate);
	
	//휴면 계정 관리 테이블 업데이트(안내 메일 발송 완료, 탈퇴 처리 완료)
	int updateDormantUser(DormantStatus changeStatus, DormantUserDto dormantUser);
	
	//휴면 계정 탈퇴 처리
	void handleWithdrawal(DormantUserDto dormantUser);
}
