import { CheckCircle2, Clock3, Droplet } from "lucide-react";
import Card from "./Card";

export default function RoutineCard({ routine, compact = false }) {
  return (
    <Card className={`routine-card${compact ? " routine-card--compact" : ""}`}>
      <div className="routine-card__icon">
        <Droplet size={30} fill="currentColor" strokeWidth={1.8} />
      </div>
      <p className="eyebrow">오늘의 추천 루틴</p>
      <h2>{routine.title}</h2>
      <p>{routine.reason}</p>
      {!compact ? (
        <div className="routine-meta">
          <span>
            <Clock3 size={15} />
            약 {routine.expectedMinutes}분
          </span>
          <span>
            <CheckCircle2 size={15} />
            루틴 1개
          </span>
        </div>
      ) : null}
    </Card>
  );
}
