package org.poolpool.mohaeng.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.type.SignupType;
import org.poolpool.mohaeng.user.type.UserRole;
import org.poolpool.mohaeng.user.type.UserStatus;
import org.poolpool.mohaeng.user.type.UserType;

import jakarta.validation.constraints.NotBlank;
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
public class UserDto {

    private Long userId;

    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    private String userPwd;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    private String profileImg;

    private UserType userType;

    private String businessNum;

    private SignupType signupType;

    private UserRole userRole;

    private UserStatus userStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDate lastLoginAt;

    private Integer withReasonId;

    private String withdrawalReason;
    
    public UserEntity toEntity() {
    	return UserEntity.builder()
    			.userId(userId)
    			.email(email)
    			.userPwd(userPwd)
    			.name(name)
    			.phone(phone)
    			.profileImg(profileImg)
    			.userType(userType)
    			.businessNum(businessNum)
    			.signupType(signupType)
    			.userRole(userRole)
    			.userStatus(userStatus)
    			.createdAt(createdAt)
    			.updatedAt(updatedAt)
    			.lastLoginAt(lastLoginAt)
    			.withReasonId(withReasonId)
    			.withdrawalReason(withdrawalReason)
    			.build();
    }
    
    public static UserDto fromEntity(UserEntity userEntity) {
    	if (userEntity == null) return null;
    	
    	return UserDto.builder()
    			.userId(userEntity.getUserId())
    			.email(userEntity.getEmail())
    			.userPwd(userEntity.getUserPwd())
    			.name(userEntity.getName())
    			.phone(userEntity.getPhone())
    			.profileImg(userEntity.getProfileImg())
    			.userType(userEntity.getUserType())
    			.businessNum(userEntity.getBusinessNum())
    			.signupType(userEntity.getSignupType())
    			.userRole(userEntity.getUserRole())
    			.userStatus(userEntity.getUserStatus())
    			.createdAt(userEntity.getCreatedAt())
    			.updatedAt(userEntity.getUpdatedAt())
    			.lastLoginAt(userEntity.getLastLoginAt())
    			.withReasonId(userEntity.getWithReasonId())
    			.withdrawalReason(userEntity.getWithdrawalReason())
    			.build();
    }
}
