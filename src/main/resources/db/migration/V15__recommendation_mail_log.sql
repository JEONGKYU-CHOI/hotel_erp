-- =============================================================================
-- V15 : 추천 메일 발송 이력 (recommendation_mail_log)  — 발송 멱등성
-- =============================================================================
--   추천 메일(4단계)은 매월 1회 발송 배치다. 배치는 재실행될 수 있다 —
--   크론 중복 기동, 수동 트리거 재호출, 실패 후 재시도. 같은 달에 같은 회원에게
--   추천 메일이 두 번 나가서는 안 된다.
--
--   ── 멱등성 ────────────────────────────────────────────────────────────────
--   UNIQUE (tenant_id, member_id, send_month) 로 "회원 × 발송월"당 한 행만 허용한다.
--   한 행 = 실제로 발송된 메일 하나다. 발송 서비스는 발송 전 이 키의 존재를 확인해
--   이미 보낸 회원을 건너뛰고, 선조회를 빠뜨려도 이 제약이 최후에 막는다
--   (야간마감 night_close 의 UNIQUE(tenant_id, business_date) 와 같은 규율, D-004).
--
--   발송에 실패하면 행을 남기지 않는다 — 다음 실행이 다시 시도할 수 있게 한다.
--   즉 이 테이블은 "성공적으로 발송된 메일"만 기록한다.
--
--   send_month 는 발송 대상 달을 그 달 1일(DATE)로 저장한다. 달력 월 단위 멱등이므로
--   일자는 의미가 없고 항상 1일로 정규화한다.
-- =============================================================================
CREATE TABLE recommendation_mail_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT       NOT NULL DEFAULT 1,
    member_id    BIGINT       NOT NULL              COMMENT '수신 회원',
    send_month   DATE         NOT NULL              COMMENT '발송 대상 달(그 달 1일로 정규화)',
    email        VARCHAR(255) NOT NULL              COMMENT '발송 시점 수신 이메일 스냅샷',
    subject      VARCHAR(255) NOT NULL              COMMENT '발송된 메일 제목',
    item_count   INT          NOT NULL DEFAULT 0    COMMENT '추천 후보 개수(1~3)',
    sent_at      DATETIME     NOT NULL              COMMENT '발송 완료 시각',

    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,

    PRIMARY KEY (id),

    -- ★ 발송 멱등성. 회원 × 발송월당 한 번. 위 주석 참조.
    UNIQUE KEY uk_recommendation_mail (tenant_id, member_id, send_month),

    CONSTRAINT fk_recommendation_mail_member
        FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '추천 메일 발송 이력. 회원 × 발송월당 한 행. 발송 멱등성의 근거.';
