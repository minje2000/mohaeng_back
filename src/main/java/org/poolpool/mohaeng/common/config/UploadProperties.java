package org.poolpool.mohaeng.common.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

// OS 변경 시 properties만 수정하면 됨. 경로 규칙이 한 곳에 모임. React 연동 API에서도 일관성 유지
@ConfigurationProperties(prefix = "app")  // 이 클래스는 properties 와 연결된다는 어노테이션임, (prefix = "app") == app.* 의미함
public record UploadProperties(
        String uploadDir   // app.upload-dir 프로퍼티 값으로 매핑됨. uploadDir == C:/upload_files (base 경로)
) {
	
	/** 행사 업로드 경로 */
	public Path boardDir() {
		return Path.of(uploadDir, "event");  // C:/upload_files/event
	}

    /** 행사 주최측 부스 업로드 경로 */
    public Path hboothDir() {
        return Path.of(uploadDir, "hbooth");  // C:/upload_files/hbooth
    }
    
    /** 부스 참여측 부스 업로드 경로 */
    public Path pboothDir() {
    	return Path.of(uploadDir, "pbooth");  // C:/upload_files/pbooth
    }

    /** 회원 사진 업로드 경로 */
    public Path photoDir() {
        return Path.of(uploadDir, "photo");  // C:/upload_files/photo
    }
}
