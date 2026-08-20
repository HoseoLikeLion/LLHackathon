import { request } from "./client";

export function createUser(nickname) {
  return request("/api/users", {
    method: "POST",
    body: nickname ? JSON.stringify({ nickname }) : undefined,
  });
}

export function getHome() {
  return request("/api/users/me/home", { requiresUser: true });
}

export function createRecord({ photo, sleepHours, hadDrinkOrSnack, stressLevel }) {
  const form = new FormData();
  form.append("photo", photo);
  form.append("sleepHours", String(sleepHours));
  form.append("hadDrinkOrSnack", String(hadDrinkOrSnack));
  form.append("stressLevel", String(stressLevel));
  return request("/api/records", { method: "POST", body: form, requiresUser: true });
}

export function getTodayRecord() {
  return request("/api/records/today", { requiresUser: true });
}

export function getTodayRoutine() {
  return request("/api/routines/today", { requiresUser: true });
}

export function completeTodayRoutine() {
  return request("/api/routines/today/complete", { method: "POST", requiresUser: true });
}

export function deferTodayRoutine() {
  return request("/api/routines/today/defer", { method: "POST", requiresUser: true });
}

export function getAlternativeRoutine() {
  return request("/api/routines/today/alternative", { method: "POST", requiresUser: true });
}

export function getReport(days = 14) {
  return request(`/api/reports/summary?days=${days}`, { requiresUser: true });
}

export function createDemoSession() {
  return request("/api/demo/session", { method: "POST" });
}
