import { Check, Droplet } from "lucide-react";
import Button from "../components/Button";
import Card from "../components/Card";
import Header from "../components/Header";

export default function RecordCompletePage({ routine, isSaving, onBack, onSave, onNavigate }) {
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
            <strong>{routine.name}</strong>
            <span>오늘 완료</span>
          </div>
          <dl>
            <div>
              <dt>상태</dt>
              <dd>저장 대기</dd>
            </div>
            <div>
              <dt>시간</dt>
              <dd>오후 10:30</dd>
            </div>
          </dl>
        </Card>

        <p className="helper-copy">오늘의 실천 기록이 저장됩니다.</p>

        <div className="cta-stack">
          <Button disabled={isSaving} onClick={onSave}>
            {isSaving ? "기록 저장 중..." : "오늘 기록 저장하기"}
          </Button>
          <Button variant="ghost" onClick={() => onNavigate("home")}>
            홈으로 돌아가기
          </Button>
        </div>
      </main>
    </>
  );
}

