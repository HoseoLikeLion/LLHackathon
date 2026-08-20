import { CheckCircle2, Clock3, Droplet } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";

export default function RoutinePage({ routine, onBack, onNavigate }) {
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
          <h2>{routine.name}</h2>
          <span className="time-pill">
            <Clock3 size={14} />
            {routine.estimatedTime}
          </span>
          <p>{routine.reason}</p>
          <ol className="step-list">
            {routine.steps.map((step, index) => (
              <li key={step}>
                <span>{index + 1}</span>
                {step}
              </li>
            ))}
          </ol>
        </Card>

        <div className="cta-stack">
          <Button onClick={() => onNavigate("complete")}>
            <CheckCircle2 size={18} />
            오늘 루틴 완료하기
          </Button>
          <Button variant="ghost" onClick={() => onNavigate("home")}>
            나중에 실천할게요
          </Button>
        </div>
      </main>
    </>
  );
}

