import { CheckCircle2, Clock3, Droplet } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";

export default function RoutinePage({ routine, error, isSaving, onBack, onComplete, onDefer }) {
  return (
    <>
      <Header showBack onBack={onBack} />
      <main className="screen routine-screen">
        <section className="page-title">
          <h1>오늘의 루틴</h1>
          <p>오늘 피부에 가장 필요한 한 가지를 실천해보세요.</p>
        </section>

        <Card className="routine-detail-card">
          <div className="routine-detail-card__icon">
            <Droplet size={34} fill="currentColor" strokeWidth={1.8} />
          </div>
          <h2>{routine.title}</h2>
          <span className="time-pill">
            <Clock3 size={14} />
            약 {routine.expectedMinutes}분
          </span>
          <p>{routine.reason}</p>
          <ol className="step-list">
            <li><span>1</span>{routine.method}</li>
          </ol>
        </Card>

        <div className="cta-stack">
          <Button disabled={isSaving || routine.status === "completed"} onClick={onComplete}>
            <CheckCircle2 size={18} />
            {routine.status === "completed" ? "오늘 루틴 완료됨" : isSaving ? "처리 중..." : "오늘 루틴 완료하기"}
          </Button>
          <Button variant="ghost" disabled={isSaving} onClick={onDefer}>
            {isSaving ? "처리 중..." : "나중에 실천할게요"}
          </Button>
          {error}
        </div>
      </main>
    </>
  );
}
