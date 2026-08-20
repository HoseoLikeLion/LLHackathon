import { mockAnalysis, mockReport } from "../data/mockData";

const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));

export async function analyzeSkinRecord(record) {
  await wait(500);

  return {
    ...mockAnalysis,
    record,
    analyzedAt: new Date().toISOString(),
  };
}

export async function saveRoutineRecord(analysis) {
  await wait(350);
  const id =
    typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `routine-${Date.now()}`;

  return {
    id,
    routineName: analysis.routine.name,
    completed: true,
    completedAt: new Date().toISOString(),
  };
}

export async function fetchMockReport() {
  await wait(250);
  return mockReport;
}
