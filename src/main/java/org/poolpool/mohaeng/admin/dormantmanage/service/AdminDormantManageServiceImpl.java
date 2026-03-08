package org.poolpool.mohaeng.admin.dormantmanage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.poolpool.mohaeng.admin.dormantmanage.dto.DormantUserDto;
import org.poolpool.mohaeng.admin.dormantmanage.entity.DormantUserEntity;
import org.poolpool.mohaeng.admin.dormantmanage.repository.AdminDormantManageRepository;
import org.poolpool.mohaeng.admin.dormantmanage.type.DormantStatus;
import org.poolpool.mohaeng.common.service.MailService;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.SocialUserRepository;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.poolpool.mohaeng.user.type.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDormantManageServiceImpl implements AdminDormantManageService {

	private final AdminDormantManageRepository adminDormantManageRepository;
	private final MailService mailService;
	private final UserRepository userRepository;
	private final SocialUserRepository socialUserRepository;

	// 휴면 계정 관리 프로시저 호출
	@Override
	@Transactional
	public void callDormantUserProc() {
		adminDormantManageRepository.callDormantUserProc();
	}

	// 휴면 계정 관리 조회
	@Override
	@Transactional(readOnly = true)
	public Page<DormantUserDto> findDormantUsers(Pageable pageable) {
		return adminDormantManageRepository.findDormantUsers(pageable);
	}

	//안내 메일 보낼 휴면 계정 조회, 탈퇴 처리할 휴면 계정 조회
	@Override
	@Transactional(readOnly = true)
	public List<DormantUserDto> findDormantUsersByDormantStatus(DormantStatus status) {
		return adminDormantManageRepository.findDormantUsersByDormantStatus(status);
	}

	// 안내 메일 전송
	@Override
	public void sendDormantMail(String email, LocalDate lastLoginDate) {

		LocalDate deleteDate = LocalDate.now().plusDays(7);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

		String subject = "[모행] 휴면 계정 전환 안내";

		String content = """
				<div style="font-family: Arial; line-height:1.6;">
				<p>안녕하세요, <b>모행</b> 입니다.</p>

				<p>
				회원님께서 <b>%s</b> 이후로 장기간 로그인 기록이 없어<br>
				현재 회원님의 계정이 휴면 상태로 전환되어 있음을 안내드립니다.
				</p>

				<p>
				관련 법령 및 내부 운영 정책에 따라,<br>
				본 메일 수신일로부터 7일 이내에 로그인하지 않으실 경우<br>
				회원님의 계정은 자동으로 탈퇴 처리되며,<br>
				탈퇴 시 회원 정보 및 이용 기록은 복구가 불가능합니다.
				</p>

				<p>
				계속해서 모행 서비스를 이용하시고자 하는 경우,<br>
				탈퇴 예정일 이전에 한 번이라도 로그인해 주시길 바랍니다.
				</p>

				<p>
				▶ 탈퇴 예정일: <b>%s</b><br>

				<br>
				<p style="font-size:12px;color:gray;">
				본 메일은 발신 전용으로, 회신이 불가한 점 양해 부탁드립니다.
				</p>

				<p>감사합니다.<br>모행 드림</p>
				</div>
				""".formatted(lastLoginDate.format(formatter), deleteDate.format(formatter));

		mailService.sendMail(email, subject, content);
	}
	
	//휴면 계정 관리 테이블 업데이트(안내 메일 발송 완료, 탈퇴 처리 완료)
	@Transactional
	public int updateDormantUser(DormantStatus changeStatus, DormantUserDto dormantUser) {
		if(changeStatus == DormantStatus.NOTIFIED) {		//안내 메일 발송 완료 처리일 경우
			dormantUser.setNotifiedAt(LocalDateTime.now());
		} else {		//안내 메일 발송 완료 처리일 경우
			dormantUser.setWithdrawnAt(LocalDateTime.now());
		}
		
		dormantUser.setDormantStatus(changeStatus);
		dormantUser.setUpdatedAt(LocalDateTime.now());
		
		UserEntity user = userRepository.findById(dormantUser.getUserId())
	            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
		
		DormantUserEntity dormantUserEntity = dormantUser.toEntity(user);
		
		return adminDormantManageRepository.save(dormantUserEntity) != null ? 1 : 0;
	}

	//휴면 계정 탈퇴 처리
	@Override
	@Transactional
	public void handleWithdrawal(DormantUserDto dormantUser) {
		Long userId = dormantUser.getUserId();
		
		boolean socialYn = socialUserRepository.existsByUser_UserId(userId);
		if(socialYn) socialUserRepository.deleteByUser_UserId(userId);
		
		UserEntity updateUser = userRepository.findById(userId)
	            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
		
		updateUser.setUserStatus(UserStatus.WITHDRAWAL);
	}

}