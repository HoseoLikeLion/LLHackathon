import { CalendarDays, Leaf } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";
import ReportChart from "../components/ReportChart";
import StatusCard from "../components/StatusCard";

const labelFromTrend = { up: "개선", down: "감소", same: "비슷" };

export default function ReportPage({ report, isLoading, error, onBack, onNavigate }) {
  const trends = report?.trends || [];
  const changes = report ? [
    { label: "붉음", value: labelFromTrend[report.latestVsPrevious.redness], trend: report.latestVsPrevious.redness, tone: "red" },
    { label: "수분", value: labelFromTrend[report.latestVsPrevious.moisture], trend: report.latestVsPrevious.moisture, tone: "blue" },
    { label: "유분", value: labelFromTrend[report.latestVsPrevious.oil], trend: report.latestVsPrevious.oil, tone: "green" },
  ] : [];
  const recentRoutine = report?.routineEffects?.[0];
  return (
    <>
      <Header showBack onBack={onBack} />
      <main className="screen report-screen">
        <section className="page-title report-title">
          <h1>피부 변화 리포트</h1>
          <span>
            <CalendarDays size={15} />
            최근 14일
          </span>
        </section>

        {isLoading ? <p className="state-message">리포트를 불러오는 중...</p> : null}
        {error ? <p className="state-message state-message--error">{error}</p> : null}
        {report && !trends.length ? <p className="state-message">아직 리포트를 만들 기록이 없어요. 오늘의 피부를 기록해 보세요.</p> : null}
        {report && trends.length ? <>
          <Card className="report-summary-card">
            <strong>최근 기록을 기준으로 피부 변화를 정리했어요.</strong>
          </Card>

        <div className="status-grid">
          {changes.map((item) => (
            <StatusCard
              key={item.label}
              label={item.label}
              value={item.value}
              tone={item.tone}
              trend={item.trend}
            />
          ))}
        </div>

        <Card className="chart-card">
          <h2>변화 추이</h2>
          <ReportChart trends={trends} />
        </Card>

        {recentRoutine ? <Card className="recent-routine-card">
          <div className="recent-routine-card__icon">
            <Leaf size={24} fill="currentColor" strokeWidth={1.7} />
          </div>
          <div>
            <p>최근 실천한 루틴</p>
            <strong>{recentRoutine.title}</strong>
            <span>최근 {recentRoutine.executedCount}회 실천</span>
            <em>{recentRoutine.note}</em>
          </div>
        </Card> : null}
        </> : null}

        <div className="cta-stack">
          <Button onClick={() => onNavigate("record")}>오늘도 피부 기록하기</Button>
          <Button variant="outline" onClick={() => onNavigate("home")}>
            홈으로 돌아가기
          </Button>
        </div>
      </main>
    </>
  );
}
