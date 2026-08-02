// 로딩 중 콘텐츠 자리를 잡아주는 스켈레톤 블록. 실제 데이터가 들어올 자리와
// 비슷한 형태를 회색 셰머로 미리 보여줘, 화면이 비거나 갑자기 튀는 것을 막는다.

// 낱개 블록. width/height 는 CSS 값 문자열(예: '60%', 20).
export function SkeletonLine({ width = '100%', height = 14, style }) {
  return (
    <span
      className="skeleton"
      aria-hidden="true"
      style={{ width, height, display: 'block', borderRadius: 6, ...style }}
    />
  )
}

// 내 예약 목록 한 줄 모양의 스켈레톤. count 개를 리스트로 찍는다.
export function SkeletonResList({ count = 3 }) {
  return (
    <ul className="res-list" aria-hidden="true">
      {Array.from({ length: count }).map((_, i) => (
        <li key={i} className="res-item">
          <div className="res-head">
            <SkeletonLine width="40%" height={16} />
            <SkeletonLine width="64px" height={22} style={{ borderRadius: 999 }} />
          </div>
          <SkeletonLine width="55%" height={18} style={{ marginTop: 10 }} />
          <SkeletonLine width="70%" height={13} style={{ marginTop: 10 }} />
        </li>
      ))}
    </ul>
  )
}
