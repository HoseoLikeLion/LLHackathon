import { Lightbulb, Sparkles } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";
import RoutineCard from "../components/RoutineCard";
import StatusCard from "../components/StatusCard";

export default function AnalysisPage({ analysis, error, isChangingRoutine, onBack, onNavigate, onAlternative }) {

  return (
    <>
      <Header title="오늘의 피부 분석" showBack showProfile={false} onBack={onBack} />
      <main className="screen analysis-screen">
        <Card className="condition-card">
          <p>{analysis.conditionLabel}</p>
          <strong>{analysis.condition}</strong>
          <div className="score-ring" style={{ "--score": `${analysis.score}%` }} aria-label={`피부 컨디션 ${analysis.score}점`}>
            <span>{analysis.score}</span>
            <small>/100</small>
          </div>
          <em>{analysis.summary}</em>
        </Card>

        <div className="status-grid analysis-status-grid">
          <StatusCard label="붉음" value={analysis.redness} tone="red" trend="up" />
          <StatusCard label="수분" value={analysis.moisture} tone="blue" trend="down" />
          <StatusCard label="유분" value={analysis.oiliness} tone="green" trend="same" />
        </div>

        <Card className="insight-card">
          <Lightbulb size={22} fill="currentColor" strokeWidth={1.8} />
          <p>{analysis.insight}</p>
        </Card>

        <RoutineCard routine={analysis.routine} compact />

        <div className="cta-stack">
          <Button onClick={() => onNavigate("routine")}>
            <Sparkles size={18} />
            오늘 이 루틴 실천하기
          </Button>
          <Button variant="secondary" disabled={isChangingRoutine} onClick={onAlternative}>
            {isChangingRoutine ? "다른 루틴을 찾는 중..." : "다른 루틴 보기"}
          </Button>
          {error}
        </div>
      </main>
    </>
  );
}
