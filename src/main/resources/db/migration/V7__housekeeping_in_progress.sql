-- =============================================================================
-- V7 : 하우스키핑 청소중 상태 (housekeeping)  ★ 청소 흐름 · D-040 ★
-- =============================================================================
--   체크아웃이 호실을 DIRTY 로 두는 것까지만 있었다(D-031). 청소 흐름을 3단계로 세운다:
--     DIRTY(청소필요) → IN_PROGRESS(청소중) → CLEAN(청소완료)
--   진행 중인 방을 구분해야 두 직원이 같은 방을 동시에 잡는 혼선을 막고, 현황판에서
--   무엇이 청소 중인지 보인다.
--
--   clean_status CHECK 제약에 IN_PROGRESS 를 더한다. MySQL 은 CHECK 를 수정할 수 없어
--   드롭 후 재생성한다. 기존 값(CLEAN/DIRTY/INSPECTED/OUT_OF_ORDER)은 그대로 유효하다.
-- =============================================================================

ALTER TABLE room DROP CHECK chk_room_clean_status;

ALTER TABLE room ADD CONSTRAINT chk_room_clean_status
    CHECK (clean_status IN ('CLEAN', 'DIRTY', 'IN_PROGRESS', 'INSPECTED', 'OUT_OF_ORDER'));
