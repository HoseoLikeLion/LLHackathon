import { Droplet, Leaf, Thermometer } from "lucide-react";
import Button from "../components/Button";
import Header from "../components/Header";
import PhotoUploader from "../components/PhotoUploader";
import SkinStatusSelector from "../components/SkinStatusSelector";
import { statusOptions } from "../data/mockData";

export default function SkinRecordPage({
  record,
  isAnalyzing,
  onBack,
  onPhotoChange,
  onRecordChange,
  onAnalyze,
}) {
  const canAnalyze = Boolean(record.photoPreview && record.redness && record.moisture && record.oiliness);

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
            title="붉음 정도"
            icon={<Thermometer size={19} />}
            options={statusOptions.redness}
            value={record.redness}
            tone="red"
            onChange={(value) => onRecordChange("redness", value)}
          />
          <SkinStatusSelector
            title="수분감"
            icon={<Droplet size={19} />}
            options={statusOptions.moisture}
            value={record.moisture}
            tone="blue"
            onChange={(value) => onRecordChange("moisture", value)}
          />
          <SkinStatusSelector
            title="유분감"
            icon={<Leaf size={19} />}
            options={statusOptions.oiliness}
            value={record.oiliness}
            tone="green"
            onChange={(value) => onRecordChange("oiliness", value)}
          />
        </section>

        <Button className="screen-cta" disabled={!canAnalyze || isAnalyzing} onClick={onAnalyze}>
          {isAnalyzing ? "AI 피부 분석 중..." : "AI 피부 분석 시작하기"}
        </Button>
      </main>
    </>
  );
}

