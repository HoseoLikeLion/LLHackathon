import { Check, Droplet } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";

export default function RecordCompletePage({ routine, onNavigate }) {
  return (
    <>
      <Header title="오늘의 기록" showBack={false} showProfile={false} />
      <main className="screen complete-screen">
        <section className="success-hero">
          <div className="success-ring">
            <Check size={54} strokeWidth={2.1} />
          </div>
          <h1>오늘의 루틴을 완료했어요!</h1>
        </section>

        <Card className="complete-card">
          <div className="complete-card__top">
            <div className="mini-icon">
              <Droplet size={20} fill="currentColor" strokeWidth={1.7} />
            </div>
            <strong>{routine.title}</strong>
            <span>오늘 완료</span>
          </div>
          <dl>
            <div>
              <dt>상태</dt>
              <dd>저장 완료</dd>
            </div>
            <div>
              <dt>시간</dt>
              <dd>{routine.completedAt ? new Intl.DateTimeFormat("ko-KR", { hour: "numeric", minute: "numeric" }).format(new Date(routine.completedAt)) : "완료"}</dd>
            </div>
          </dl>
        </Card>

        <p className="helper-copy">오늘의 실천 기록이 저장되었어요.</p>

        <div className="cta-stack">
          <Button onClick={() => onNavigate("report")}>
            피부 변화 리포트 보기
          </Button>
          <Button variant="ghost" onClick={() => onNavigate("home")}>
            홈으로 돌아가기
          </Button>
        </div>
      </main>
    </>
  );
}
