package uk.gov.moj.cpp.bulkscan.azure;

public enum ErrorCode {

    UNSUPPORTED_FILE_TYPE(4001),
    NO_PROSECUTOR_FOUND_FOR_SENDER_DOMAIN(4002),
    CASE_URN_NOT_FOUND(4003),
    PROSECUTOR_FOUND_BUT_IS_UNRELATED_TO_CASE_URN(4004),
    NO_SJPN_FILE_ATTACHED_TO_THE_EMAIL(4005);

    public Integer getCode() {
        return code;
    }

    private final Integer code;

    ErrorCode(final Integer code) {
        this.code = code;
    }

}
