-- =============================================================================
-- 데모 시드 (1회용) — 부킹엔진·관리자 대시보드 데모용 기준 데이터.
--   새 객실타입 3종(스탠다드 트윈·디럭스 더블·이그제큐티브 스위트)·요금제·호실·재고
--   (오늘~+60일)를 넣고, 기존 테스트 타입은 active=0 으로 숨긴다.
--
--   실행(한글 인코딩 함정 회피 — 반드시 파일 + charset 지정):
--     mysql -u root --default-character-set=utf8mb4 hotel_erp -e "source scripts/dev-seed-demo.sql"
--   ⚠ 코드 UNIQUE 제약이 있어 같은 DB 에 두 번 돌리면 실패한다(1회용). 날짜는 갱신 필요할 수 있음.
-- =============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE room_type SET active = 0;

INSERT INTO room_type (tenant_id, code, name, description, bed_type, standard_occupancy, max_occupancy, display_order, active, created_at, updated_at, created_by) VALUES
 (1,'STDT','스탠다드 트윈','도심 전망의 아늑한 기본 객실. 트윈 베드로 편안한 하룻밤.','트윈',2,2,10,1,NOW(),NOW(),'SEED'),
 (1,'DLXD','디럭스 더블','넓은 창과 킹 사이즈 더블 베드를 갖춘 디럭스 객실.','더블',2,3,20,1,NOW(),NOW(),'SEED'),
 (1,'EXSU','이그제큐티브 스위트','거실이 분리된 최상층 스위트. 파노라마 시티뷰.','킹',2,4,30,1,NOW(),NOW(),'SEED');

INSERT INTO rate_plan (tenant_id, room_type_id, code, name, base_amount, breakfast_included, refundable, cancel_deadline_days, penalty_rate, active, created_at, updated_at, created_by)
SELECT 1, id, 'STDT-BAR','기본요금',150000,0,1,1,0,1,NOW(),NOW(),'SEED' FROM room_type WHERE code='STDT'
UNION ALL SELECT 1, id, 'STDT-BF','조식 포함',175000,1,1,1,0,1,NOW(),NOW(),'SEED' FROM room_type WHERE code='STDT'
UNION ALL SELECT 1, id, 'DLXD-BAR','기본요금',220000,0,1,1,0,1,NOW(),NOW(),'SEED' FROM room_type WHERE code='DLXD'
UNION ALL SELECT 1, id, 'DLXD-BF','조식 포함',250000,1,1,1,0,1,NOW(),NOW(),'SEED' FROM room_type WHERE code='DLXD'
UNION ALL SELECT 1, id, 'EXSU-BAR','기본요금',420000,1,1,1,0,1,NOW(),NOW(),'SEED' FROM room_type WHERE code='EXSU'
UNION ALL SELECT 1, id, 'EXSU-NRF','논리펀더블 특가',360000,1,0,1,100,1,NOW(),NOW(),'SEED' FROM room_type WHERE code='EXSU';

INSERT INTO room (tenant_id, room_type_id, room_no, floor, occupancy_status, clean_status, active, created_at, updated_at, created_by)
SELECT 1, id, '301',3,'VACANT','CLEAN',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='STDT'
UNION ALL SELECT 1, id,'302',3,'VACANT','DIRTY',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='STDT'
UNION ALL SELECT 1, id,'303',3,'VACANT','IN_PROGRESS',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='STDT'
UNION ALL SELECT 1, id,'304',3,'VACANT','INSPECTED',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='STDT'
UNION ALL SELECT 1, id,'501',5,'VACANT','CLEAN',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='DLXD'
UNION ALL SELECT 1, id,'502',5,'VACANT','DIRTY',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='DLXD'
UNION ALL SELECT 1, id,'503',5,'VACANT','OUT_OF_ORDER',0,NOW(),NOW(),'SEED' FROM room_type WHERE code='DLXD'
UNION ALL SELECT 1, id,'1001',10,'VACANT','CLEAN',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='EXSU'
UNION ALL SELECT 1, id,'1002',10,'VACANT','INSPECTED',1,NOW(),NOW(),'SEED' FROM room_type WHERE code='EXSU';

INSERT INTO room_inventory (tenant_id, room_type_id, stay_date, total_qty, sold_qty, held_qty, created_at, updated_at, created_by)
WITH RECURSIVE d AS (
  SELECT DATE('2026-07-31') AS dt
  UNION ALL SELECT dt + INTERVAL 1 DAY FROM d WHERE dt < DATE('2026-09-29')
)
SELECT 1, rt.id, d.dt,
  CASE rt.code WHEN 'STDT' THEN 5 WHEN 'DLXD' THEN 4 WHEN 'EXSU' THEN 2 END,
  0, 0, NOW(), NOW(), 'SEED'
FROM d CROSS JOIN room_type rt
WHERE rt.code IN ('STDT','DLXD','EXSU');

COMMIT;

SELECT id, code, name, active FROM room_type WHERE code IN ('STDT','DLXD','EXSU');
SELECT COUNT(*) AS inv_rows, MIN(stay_date) AS min_d, MAX(stay_date) AS max_d FROM room_inventory WHERE created_by='SEED';
SELECT clean_status, COUNT(*) AS n FROM room WHERE created_by='SEED' GROUP BY clean_status;
