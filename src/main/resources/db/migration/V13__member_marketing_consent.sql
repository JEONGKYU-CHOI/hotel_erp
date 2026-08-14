-- 마케팅 수신 동의 + 수신거부 토큰 (추천 메일 발송의 전제).
--   marketing_consent : 동의 여부. 미동의 기본값(false) — 명시적으로 체크한 회원만 true.
--   unsubscribe_token : 메일 하단 수신거부 링크의 인증 키. 가입 시 발급, 로그인 없이 옵트아웃에 쓴다.
-- 기존 회원은 동의하지 않은 것으로 본다(false). 토큰은 신규 가입부터 채운다.
ALTER TABLE member
    ADD COLUMN marketing_consent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '마케팅 수신 동의' AFTER birth_date,
    ADD COLUMN unsubscribe_token CHAR(36)   NULL COMMENT '수신거부 링크 토큰(UUID)'      AFTER marketing_consent,
    ADD UNIQUE KEY uk_member_unsubscribe_token (unsubscribe_token);
