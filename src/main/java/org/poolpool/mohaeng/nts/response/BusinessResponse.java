package org.poolpool.mohaeng.nts.response;

import java.util.List;

import lombok.Data;

@Data
public class BusinessResponse {
	private List<BusinessData> data;

    @Data
    public static class BusinessData {
        private String b_no;
        private String b_stt;
        private String b_stt_cd;
        private String tax_type;
        private String tax_type_cd;
        private String end_dt;
    }
}
