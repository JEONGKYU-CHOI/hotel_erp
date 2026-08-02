-- =============================================================================
-- 객실타입·요금정책에 영문 표시명(name_en) 추가 — 부킹엔진 다국어(한/영).
--
--   nullable 이다. 비어 있으면 부킹엔진이 한글명(name)으로 폴백하므로, 기존 데이터나
--   영문명을 아직 안 넣은 정책도 깨지지 않는다. PMS 등록 화면에서 영문명을 함께 입력한다.
--
--   아래 UPDATE 는 시드 기준데이터(코드가 고정된 데모/개발 데이터)의 영문명을 채운다.
--   이미 시딩된 볼륨(멱등 시더가 다시 돌지 않는)에서도 영문이 바로 뜨게 하기 위함이며,
--   NULL 인 행만 건드려 관리자가 직접 넣은 값을 덮지 않는다. 시드가 없는 환경(운영·테스트)
--   에서는 매칭되는 행이 없어 아무 것도 바꾸지 않는다.
-- =============================================================================
ALTER TABLE room_type ADD COLUMN name_en VARCHAR(100) NULL AFTER name;
ALTER TABLE rate_plan ADD COLUMN name_en VARCHAR(100) NULL AFTER name;

UPDATE room_type SET name_en = 'Standard Twin'   WHERE code = 'STDT' AND name_en IS NULL;
UPDATE room_type SET name_en = 'Deluxe Double'   WHERE code = 'DLXD' AND name_en IS NULL;
UPDATE room_type SET name_en = 'Executive Suite' WHERE code = 'EXSU' AND name_en IS NULL;

UPDATE rate_plan SET name_en = 'Standard Rate'        WHERE code IN ('STDT-BAR','DLXD-BAR','EXSU-BAR') AND name_en IS NULL;
UPDATE rate_plan SET name_en = 'Breakfast Included'   WHERE code IN ('STDT-BF','DLXD-BF')             AND name_en IS NULL;
UPDATE rate_plan SET name_en = 'Non-Refundable Saver' WHERE code = 'EXSU-NRF'                          AND name_en IS NULL;
