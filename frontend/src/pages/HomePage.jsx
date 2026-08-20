import { ArrowRight } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";
import StatusCard from "../components/StatusCard";

export default function HomePage({ home, analysis, isLoading, error, onNavigate, onStartDemo, isStartingDemo }) {
  const hasTodayRecord = Boolean(home?.todayRecorded && analysis);
  return (
    <>
      <Header showBack={false} />
      <main className="screen">
        <section className="hero-copy">
          <p>안녕하세요, {home?.nickname || "사자님"}</p>
          <h1>오늘 내 피부는 어떨까요?</h1>
          <span>20초만 기록하면 오늘 필요한 관리를 알려드릴게요.</span>
        </section>

        <Card className="home-hero-card">
          <img src="/skin-illustration.svg" alt="부드러운 피부 케어 일러스트" />
          <Button onClick={() => onNavigate(home?.todayRecorded ? "analysis" : "record")}>
            {home?.todayRecorded ? "오늘의 분석 다시 보기" : "지금 피부 기록하기"}
            <ArrowRight size={18} />
          </Button>
        </Card>
        <Button variant="outline" onClick={onStartDemo} disabled={isStartingDemo}>
          {isStartingDemo ? "데모 데이터 준비 중..." : "데모 데이터로 리포트 보기"}
        </Button>

        <section className="section-heading">
          <h2>오늘의 피부 상태</h2>
          <button type="button" disabled={!hasTodayRecord} onClick={() => onNavigate("analysis")}>
            오늘의 분석 다시 보기
          </button>
        </section>
        <div className="status-grid">
          <StatusCard label="붉음" value={hasTodayRecord ? analysis.redness : "기록 없음"} tone="red" trend="same" />
          <StatusCard label="수분" value={hasTodayRecord ? analysis.moisture : "기록 없음"} tone="blue" trend="same" />
          <StatusCard label="유분" value={hasTodayRecord ? analysis.oiliness : "기록 없음"} tone="green" trend="same" />
        </div>
        {isLoading ? <p className="state-message">사용자 정보를 불러오는 중...</p> : null}
        {error ? <p className="state-message state-message--error">{error}</p> : null}
      </main>
    </>
  );
}
