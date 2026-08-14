-- 회원 프로필 확장: 성별·생년월일 (추후 나이/성별 기반 추천 메일 기능의 근거 데이터).
-- 기존 회원(데모 시드 포함)이 있으므로 NULL 허용으로 추가한다 — 신규 가입은 폼/DTO 에서 필수로 받는다.
ALTER TABLE member
    ADD COLUMN gender     VARCHAR(10) NULL COMMENT '성별: MALE / FEMALE / OTHER' AFTER phone,
    ADD COLUMN birth_date DATE        NULL COMMENT '생년월일 (나이 계산용)'        AFTER gender,
    ADD CONSTRAINT chk_member_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'));
