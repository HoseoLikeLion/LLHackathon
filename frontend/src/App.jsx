import { useEffect, useMemo, useState } from "react";
import { ApiError, getStoredUserId, setStoredUserId } from "./api/client";
import {
  completeTodayRoutine,
  createDemoSession,
  createRecord,
  createUser,
  deferTodayRoutine,
  getAlternativeRoutine,
  getHome,
  getReport,
  getTodayRecord,
  getTodayRoutine,
} from "./api/skinService";
import BottomNavigation from "./components/BottomNavigation";
import AnalysisPage from "./pages/AnalysisPage";
import HomePage from "./pages/HomePage";
import RecordCompletePage from "./pages/RecordCompletePage";
import ReportPage from "./pages/ReportPage";
import RoutinePage from "./pages/RoutinePage";
import SkinRecordPage from "./pages/SkinRecordPage";

const allowedRoutes = new Set(["home", "record", "analysis", "routine", "complete", "report"]);
const emptyRecord = { photo: null, photoPreview: "", sleepHours: null, hadDrinkOrSnack: null, stressLevel: null };

function routeFromHash() {
  const route = window.location.hash.replace("#/", "") || "home";
  return allowedRoutes.has(route) ? route : "home";
}

function navSection(route) {
  if (route === "report") return "report";
  return ["record", "analysis", "routine", "complete"].includes(route) ? "record" : "home";
}

function level(value, type) {
  if (type === "moisture") return value <= 2 ? "부족" : value >= 4 ? "충분" : "보통";
  return value <= 2 ? "낮음" : value >= 4 ? "높음" : "보통";
}

function toAnalysis(detail) {
  const { analysis, routine } = detail;
  return {
    score: analysis.score,
    conditionLabel: "오늘 피부 컨디션",
    condition: `오늘 피부 컨디션은 ${analysis.score}점이에요`,
    summary: analysis.labels.length ? analysis.labels.join(" · ") : "오늘의 피부 상태를 분석했어요.",
    redness: level(analysis.levels.redness, "redness"),
    moisture: level(analysis.levels.moisture, "moisture"),
    oiliness: level(analysis.levels.oil, "oil"),
    insight: analysis.insight,
    routine,
  };
}

export default function App() {
  const [route, setRoute] = useState(routeFromHash);
  const [record, setRecord] = useState(emptyRecord);
  const [home, setHome] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [report, setReport] = useState(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isChangingRoutine, setIsChangingRoutine] = useState(false);
  const [isLoadingReport, setIsLoadingReport] = useState(false);
  const [isStartingDemo, setIsStartingDemo] = useState(false);
  const [error, setError] = useState("");

  const loadUserData = async () => {
    const userId = getStoredUserId();
    if (!userId) {
      const created = await createUser();
      setStoredUserId(created.userId);
    }
    const nextHome = await getHome();
    setHome(nextHome);
    if (nextHome.todayRecorded) {
      const detail = await getTodayRecord();
      setAnalysis(toAnalysis(detail));
    } else {
      setAnalysis(null);
    }
  };

  useEffect(() => {
    const initialize = async () => {
      setIsInitializing(true);
      setError("");
      try {
        await loadUserData();
      } catch (nextError) {
        if (nextError instanceof ApiError && nextError.status === 401) {
          try {
            const created = await createUser();
            setStoredUserId(created.userId);
            await loadUserData();
          } catch (retryError) {
            setError(retryError.message);
          }
        } else {
          setError(nextError.message);
        }
      } finally {
        setIsInitializing(false);
      }
    };
    initialize();
  }, []);

  useEffect(() => {
    const handleHashChange = () => setRoute(routeFromHash());
    window.addEventListener("hashchange", handleHashChange);
    if (!window.location.hash) window.location.hash = "/home";
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  const activeNav = useMemo(() => navSection(route), [route]);
  const navigate = (nextRoute) => {
    setError("");
    window.location.hash = `/${nextRoute}`;
    setRoute(nextRoute);
    requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: "smooth" }));
  };
  const goBack = () => navigate({ record: "home", analysis: "home", routine: "analysis", complete: "routine", report: "home" }[route] ?? "home");

  const handlePhotoChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setRecord((current) => {
      if (current.photoPreview) URL.revokeObjectURL(current.photoPreview);
      return { ...current, photo: file, photoPreview: URL.createObjectURL(file) };
    });
  };

  const handleAnalyze = async () => {
    if (isAnalyzing) return;
    setIsAnalyzing(true);
    setError("");
    try {
      const detail = await createRecord(record);
      setAnalysis(toAnalysis(detail));
      const nextHome = await getHome();
      setHome(nextHome);
      navigate("analysis");
    } catch (nextError) {
      if (nextError.code === "ALREADY_RECORDED") {
        try {
          const detail = await getTodayRecord();
          setAnalysis(toAnalysis(detail));
          setHome(await getHome());
          navigate("analysis");
          return;
        } catch (todayError) {
          setError(todayError.message);
        }
      } else {
        setError(nextError.message);
      }
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleAlternativeRoutine = async () => {
    setIsChangingRoutine(true);
    setError("");
    try {
      const routine = await getAlternativeRoutine();
      setAnalysis((current) => ({ ...current, routine }));
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setIsChangingRoutine(false);
    }
  };

  const handleCompleteRoutine = async () => {
    setIsSaving(true);
    setError("");
    try {
      const action = await completeTodayRoutine();
      setAnalysis((current) => ({ ...current, routine: { ...current.routine, ...action } }));
      setHome((current) => ({ ...current, streakDays: action.streakDays }));
      navigate("complete");
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeferRoutine = async () => {
    setIsSaving(true);
    setError("");
    try {
      const action = await deferTodayRoutine();
      setAnalysis((current) => ({ ...current, routine: { ...current.routine, ...action } }));
      setHome((current) => ({ ...current, streakDays: action.streakDays }));
      navigate("home");
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setIsSaving(false);
    }
  };

  const loadReport = async () => {
    setIsLoadingReport(true);
    setError("");
    try {
      setReport(await getReport());
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setIsLoadingReport(false);
    }
  };

  const handleNavigate = async (nextRoute) => {
    if (nextRoute === "routine") {
      setIsChangingRoutine(true);
      setError("");
      try {
        const routine = await getTodayRoutine();
        setAnalysis((current) => current ? { ...current, routine } : current);
      } catch (nextError) {
        setError(nextError.message);
        return;
      } finally {
        setIsChangingRoutine(false);
      }
    }
    navigate(nextRoute);
    if (nextRoute === "report") loadReport();
  };

  const handleStartDemo = async () => {
    setIsStartingDemo(true);
    setError("");
    try {
      const demo = await createDemoSession();
      setStoredUserId(demo.userId);
      await loadUserData();
      navigate("report");
      await loadReport();
    } catch (nextError) {
      setError(nextError.message);
    } finally {
      setIsStartingDemo(false);
    }
  };

  const pageError = error ? <p className="state-message state-message--error">{error}</p> : null;
  const routine = analysis?.routine;

  return (
    <div className="app-root">
      <div className="phone-frame">
        {route === "home" ? <HomePage home={home} analysis={analysis} isLoading={isInitializing} error={error} onNavigate={handleNavigate} onStartDemo={handleStartDemo} isStartingDemo={isStartingDemo} /> : null}
        {route === "record" ? <SkinRecordPage record={record} isAnalyzing={isAnalyzing} error={error} onBack={goBack} onPhotoChange={handlePhotoChange} onRecordChange={(key, value) => setRecord((current) => ({ ...current, [key]: value }))} onAnalyze={handleAnalyze} /> : null}
        {route === "analysis" && analysis ? <AnalysisPage analysis={analysis} error={pageError} isChangingRoutine={isChangingRoutine} onBack={goBack} onNavigate={handleNavigate} onAlternative={handleAlternativeRoutine} /> : null}
        {route === "routine" && routine ? <RoutinePage routine={routine} error={pageError} isSaving={isSaving} onBack={goBack} onComplete={handleCompleteRoutine} onDefer={handleDeferRoutine} /> : null}
        {route === "complete" && routine ? <RecordCompletePage routine={routine} onNavigate={handleNavigate} /> : null}
        {route === "report" ? <ReportPage report={report} isLoading={isLoadingReport} error={error} onBack={goBack} onNavigate={handleNavigate} /> : null}
        <BottomNavigation active={activeNav} onNavigate={handleNavigate} />
      </div>
    </div>
  );
}
