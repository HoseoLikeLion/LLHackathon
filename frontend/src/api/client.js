const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "https://llhackathon.onrender.com").replace(/\/$/, "");
const USER_ID_KEY = "userId";

export class ApiError extends Error {
  constructor(message, { code, status } = {}) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

export function getStoredUserId() {
  return window.localStorage.getItem(USER_ID_KEY);
}

export function setStoredUserId(userId) {
  window.localStorage.setItem(USER_ID_KEY, userId);
}

export function clearStoredUserId() {
  window.localStorage.removeItem(USER_ID_KEY);
}

export async function request(path, { method = "GET", body, headers = {}, requiresUser = false } = {}) {
  const requestHeaders = new Headers(headers);
  const userId = getStoredUserId();

  if (requiresUser && userId) {
    requestHeaders.set("X-User-Id", userId);
  }

  if (body && !(body instanceof FormData) && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { method, headers: requestHeaders, body });
  } catch {
    throw new ApiError("서버에 연결할 수 없어요. 네트워크 상태를 확인해 주세요.");
  }

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const error = payload?.error;
    if (response.status === 401) {
      clearStoredUserId();
    }
    throw new ApiError(error?.message || "요청을 처리하지 못했어요.", {
      code: error?.code,
      status: response.status,
    });
  }

  return payload;
}
