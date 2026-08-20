import { CalendarDays, Leaf } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";
import ReportChart from "../components/ReportChart";
import StatusCard from "../components/StatusCard";

export default function ReportPage({ report, onBack, onNavigate }) {
  return (
    <>
      <Header showBack onBack={onBack} />
      <main className="screen report-screen">
        <section className="page-title report-title">
          <h1>피부 변화 리포트</h1>
          <span>
            <CalendarDays size={15} />
            {report.period}
          </span>
        </section>

        <Card className="report-summary-card">
          <strong>{report.summary}</strong>
        </Card>

        <div className="status-grid">
          {report.changes.map((item) => (
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
          <ReportChart />
        </Card>

        <Card className="recent-routine-card">
          <div className="recent-routine-card__icon">
            <Leaf size={24} fill="currentColor" strokeWidth={1.7} />
          </div>
          <div>
            <p>최근 실천한 루틴</p>
            <strong>{report.recentRoutine.name}</strong>
            <span>{report.recentRoutine.count}</span>
            <em>{report.recentRoutine.note}</em>
          </div>
        </Card>

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

