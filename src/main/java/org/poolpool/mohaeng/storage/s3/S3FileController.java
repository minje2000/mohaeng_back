package org.poolpool.mohaeng.storage.s3;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class S3FileController {

    private static final Duration CACHE_MAX_AGE = Duration.ofDays(30);

    private final S3StorageService s3StorageService;

    @GetMapping({"/upload_files/event/**"})
    public ResponseEntity<?> event(HttpServletRequest request) {
        return serve(request, "event");
    }

    @GetMapping({"/upload_files/hbooth/**", "/upload_files/host-booth/**"})
    public ResponseEntity<?> hbooth(HttpServletRequest request) {
        return serve(request, "host-booth");
    }

    @GetMapping({"/upload_files/pbooth/**", "/upload_files/participant-booth/**"})
    public ResponseEntity<?> pbooth(HttpServletRequest request) {
        return serve(request, "participant-booth");
    }

    @GetMapping({"/upload_files/photo/**", "/upload_files/profile/**"})
    public ResponseEntity<?> photo(HttpServletRequest request) {
        return serve(request, "photo");
    }

    private ResponseEntity<?> serve(HttpServletRequest request, String s3Dir) {
        String storedValue = extractStoredValue(request);
        if (!StringUtils.hasText(storedValue)) {
            return ResponseEntity.notFound().build();
        }

        String key = s3StorageService.resolveKey(s3Dir, storedValue);
        if (!StringUtils.hasText(key) || !s3StorageService.exists(key)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = s3StorageService.getBytes(key);
        String contentType = s3StorageService.getContentType(key);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE).cachePublic())
                .contentType(parseMediaType(contentType))
                .body(new ByteArrayResource(bytes));
    }

    private String extractStoredValue(HttpServletRequest request) {
        String uri = request.getRequestURI().replace('\\', '/');
        String marker = "/upload_files/";
        int markerIdx = uri.indexOf(marker);
        if (markerIdx < 0) {
            return null;
        }

        String after = uri.substring(markerIdx + marker.length());
        int firstSlash = after.indexOf('/');
        if (firstSlash < 0 || firstSlash == after.length() - 1) {
            return null;
        }

        return after.substring(firstSlash + 1);
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
