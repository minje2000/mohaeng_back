package org.poolpool.mohaeng.nts.controller;

import org.poolpool.mohaeng.common.api.ApiResponse;
import org.poolpool.mohaeng.nts.service.NtsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/nts")
@RequiredArgsConstructor
public class NtsController {

    private final NtsService ntsService;

    @PostMapping("/status")
    public ResponseEntity<ApiResponse<String>> checkStatus(@RequestParam(name = "bno") String bno) {
        int result = ntsService.getStatus(bno);
//        System.out.println("bno: " + bno + ",result: " + result);
        if(result == 1) {
        	return ResponseEntity.ok(ApiResponse.ok("사업자 등록번호 조회 완료", "확인되었습니다."));
        }else if(result == 2 || result == 3){
        	return ResponseEntity.ok(ApiResponse.fail("사업자 등록번호 조회 완료", "휴업자/폐업자로 가입이 불가합니다."));
        } else if(result == -99){
        	return ResponseEntity.ok(ApiResponse.fail("사업자 등록번호 조회 완료", "존재하지 않는 사업자 등록번호입니다."));
        } else {
        	return ResponseEntity.ok(ApiResponse.fail("사업자 등록번호 조회 실패", "사업자 등록번호 조회 중 오류가 발생했습니다."));
        }
    }
}
