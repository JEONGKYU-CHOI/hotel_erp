-- =============================================================================
-- 객실타입 이미지 최대 3장 — 기존 image_url(1번) 에 2·3번을 더한다(부킹엔진 갤러리 캐러셀).
--
--   전용 이미지 테이블 대신 컬럼 3개로 둔다. "객실당 최대 3장"이라는 고정 상한이 명확하고,
--   부킹엔진·PMS 폼 모두 단순해진다. 상한이 가변이 되면 그때 room_type_image 테이블로 뺀다.
--   전부 nullable — 비면 부킹엔진이 큐레이션 폴백 이미지를 쓴다(D-052).
-- =============================================================================
ALTER TABLE room_type ADD COLUMN image_url2 VARCHAR(500) NULL AFTER image_url;
ALTER TABLE room_type ADD COLUMN image_url3 VARCHAR(500) NULL AFTER image_url2;
