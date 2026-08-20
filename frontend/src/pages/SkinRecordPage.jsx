import { Droplet, Leaf, Thermometer } from "lucide-react";
import Button from "../components/Button";
import Header from "../components/Header";
import PhotoUploader from "../components/PhotoUploader";
import SkinStatusSelector from "../components/SkinStatusSelector";
import { recordOptions } from "../data/recordOptions";

export default function SkinRecordPage({
  record,
  isAnalyzing,
  error,
  onBack,
  onPhotoChange,
  onRecordChange,
  onAnalyze,
}) {
  const canAnalyze = Boolean(record.photo && record.sleepHours !== null && record.hadDrinkOrSnack !== null && record.stressLevel !== null);

  return (
    <>
      <Header showBack onBack={onBack} />
      <main className="screen">
        <section className="page-title">
          <h1>오늘의 피부 기록</h1>
          <p>20초만 기록하면 오늘 필요한 관리를 알려드릴게요.</p>
        </section>

        <PhotoUploader previewUrl={record.photoPreview} onChange={onPhotoChange} />

        <section className="record-section">
          <h2>현재 피부 상태 체크</h2>
          <SkinStatusSelector
            title="수면 시간"
            icon={<Thermometer size={19} />}
            options={recordOptions.sleepHours}
            value={record.sleepHours}
            tone="red"
            onChange={(value) => onRecordChange("sleepHours", value)}
          />
          <SkinStatusSelector
            title="간식 또는 음주"
            icon={<Droplet size={19} />}
            options={recordOptions.hadDrinkOrSnack}
            value={record.hadDrinkOrSnack}
            tone="blue"
            onChange={(value) => onRecordChange("hadDrinkOrSnack", value)}
          />
          <SkinStatusSelector
            title="스트레스 정도"
            icon={<Leaf size={19} />}
            options={recordOptions.stressLevel}
            value={record.stressLevel}
            tone="green"
            onChange={(value) => onRecordChange("stressLevel", value)}
          />
        </section>

        <Button className="screen-cta" disabled={!canAnalyze || isAnalyzing} onClick={onAnalyze}>
          {isAnalyzing ? "AI 피부 분석 중..." : "AI 피부 분석 시작하기"}
        </Button>
        {error ? <p className="state-message state-message--error">{error}</p> : null}
      </main>
    </>
  );
}
