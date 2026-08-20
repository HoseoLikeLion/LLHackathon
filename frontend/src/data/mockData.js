export const statusOptions = {
  redness: ["낮음", "보통", "높음"],
  moisture: ["충분", "보통", "부족"],
  oiliness: ["적음", "보통", "많음"],
};

export const mockAnalysis = {
  score: 72,
  conditionLabel: "오늘 피부 컨디션",
  condition: "오늘 피부 컨디션은 72점이에요",
  summary: "오늘은 피부가 조금 예민한 상태예요.",
  redness: "증가",
  moisture: "부족",
  oiliness: "보통",
  insight: "최근 수면 부족과 피부 붉음 증가가 함께 나타났어요.",
  routine: {
    name: "수분 진정 케어",
    description: "오늘은 붉음과 수분 부족을 먼저 관리해보세요.",
    reason: "오늘은 붉음과 수분 부족이 함께 나타났어요.",
    steps: ["세안하기", "수분 크림을 충분히 바르기"],
    estimatedTime: "약 1분",
  },
};

export const mockReport = {
  period: "최근 2주",
  summary: "지난 기록보다 피부 상태가 좋아졌어요.",
  changes: [
    { label: "붉음", value: "감소", trend: "down", tone: "red" },
    { label: "수분", value: "개선", trend: "up", tone: "blue" },
    { label: "유분", value: "비슷", trend: "same", tone: "green" },
  ],
  recentRoutine: {
    name: "수분 진정 케어",
    count: "최근 3회 실천",
    note: "붉음 감소와 함께 나타났어요.",
  },
};

