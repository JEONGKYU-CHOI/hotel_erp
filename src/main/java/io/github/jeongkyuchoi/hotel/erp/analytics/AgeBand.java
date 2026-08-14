package io.github.jeongkyuchoi.hotel.erp.analytics;

/**
 * 판매 지표 세그먼트의 나이대 구간(추천 메일 근거 데이터). 10년 단위로 20/30/40/50대+ 로만 나눈다.
 *
 * <p><b>50대+ 를 한 칸으로 합치는 이유</b> — 60대 이상 표본이 얇아 구간을 더 쪼개면 세그먼트당
 * 건수가 통계적으로 무의미해진다. 추천 랭킹은 세그먼트별로 인기 객실을 뽑는 것이므로 표본이
 * 모이는 굵은 구간이 낫다.
 *
 * <p><b>20대 이하를 20대로 접는 이유</b> — 미성년 예약은 사실상 없고(시연 데이터에도 없다),
 * 있더라도 별도 마케팅 대상이 아니라 가장 낮은 성인 구간에 합쳐 다룬다.
 */
public enum AgeBand {

	/** 20대 이하(29세까지). */
	TWENTIES(20),
	THIRTIES(30),
	FORTIES(40),
	/** 50대 이상(상한 없음). */
	FIFTIES_PLUS(50);

	private final int floor;

	AgeBand(int floor) {
		this.floor = floor;
	}

	/** 이 구간의 하한 나이(20/30/40/50). 표시·정렬에 쓴다. */
	public int floor() {
		return floor;
	}

	/**
	 * 만나이를 나이대 구간으로 접는다. 29세 이하는 모두 {@link #TWENTIES}, 50세 이상은 모두
	 * {@link #FIFTIES_PLUS} 로 접힌다.
	 */
	public static AgeBand fromAge(int age) {
		if (age >= 50) {
			return FIFTIES_PLUS;
		}
		if (age >= 40) {
			return FORTIES;
		}
		if (age >= 30) {
			return THIRTIES;
		}
		return TWENTIES;
	}
}
