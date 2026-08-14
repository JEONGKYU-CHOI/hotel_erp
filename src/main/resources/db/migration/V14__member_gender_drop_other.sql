-- 성별을 남자/여자 2종으로 확정 — 'OTHER' 제거.
-- 기존에 OTHER 로 저장된 회원이 있다면 NULL 로 되돌린다(데모/신규뿐이라 사실상 없음).
UPDATE member SET gender = NULL WHERE gender = 'OTHER';

ALTER TABLE member DROP CONSTRAINT chk_member_gender;
ALTER TABLE member
    ADD CONSTRAINT chk_member_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
