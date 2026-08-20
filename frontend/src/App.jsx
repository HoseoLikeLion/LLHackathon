import { useEffect, useMemo, useState } from "react";
import { analyzeSkinRecord, fetchMockReport, saveRoutineRecord } from "./api/mockSkinService";
import BottomNavigation from "./components/BottomNavigation";
import { mockAnalysis, mockReport } from "./data/mockData";
import AnalysisPage from "./pages/AnalysisPage";
import HomePage from "./pages/HomePage";
import RecordCompletePage from "./pages/RecordCompletePage";
import ReportPage from "./pages/ReportPage";
import RoutinePage from "./pages/RoutinePage";
import SkinRecordPage from "./pages/SkinRecordPage";

const allowedRoutes = new Set(["home", "record", "analysis", "routine", "complete", "report"]);

function routeFromHash() {
  const route = window.location.hash.replace("#/", "") || "home";
  return allowedRoutes.has(route) ? route : "home";
}

function navSection(route) {
  if (route === "report") {
    return "report";
  }

  if (route === "record" || route === "analysis" || route === "routine" || route === "complete") {
    return "record";
  }

  return "home";
}

export default function App() {
  const [route, setRoute] = useState(routeFromHash);
  const [record, setRecord] = useState({
    photo: null,
    photoPreview: "",
    redness: "",
    moisture: "",
    oiliness: "",
  });
  const [analysis, setAnalysis] = useState(mockAnalysis);
  const [report, setReport] = useState(mockReport);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const handleHashChange = () => setRoute(routeFromHash());
    window.addEventListener("hashchange", handleHashChange);

    if (!window.location.hash) {
      window.location.hash = "/home";
    }

    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  const activeNav = useMemo(() => navSection(route), [route]);

  const navigate = (nextRoute) => {
    window.location.hash = `/${nextRoute}`;
    setRoute(nextRoute);
    requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: "smooth" }));
  };

  const goBack = () => {
    const backMap = {
      record: "home",
      analysis: "record",
      routine: "analysis",
      complete: "routine",
      report: "home",
    };

    navigate(backMap[route] ?? "home");
  };

  const handlePhotoChange = (event) => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    setRecord((current) => {
      if (current.photoPreview) {
        URL.revokeObjectURL(current.photoPreview);
      }

      return {
        ...current,
        photo: file,
        photoPreview: URL.createObjectURL(file),
      };
    });
  };

  const handleRecordChange = (key, value) => {
    setRecord((current) => ({
      ...current,
      [key]: value,
    }));
  };

  const handleAnalyze = async () => {
    setIsAnalyzing(true);
    try {
      const nextAnalysis = await analyzeSkinRecord(record);
      setAnalysis(nextAnalysis);
      navigate("analysis");
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleSaveRoutine = async () => {
    setIsSaving(true);
    try {
      await saveRoutineRecord(analysis);
      const nextReport = await fetchMockReport();
      setReport(nextReport);
      navigate("report");
    } finally {
      setIsSaving(false);
    }
  };

  const routine = analysis.routine;

  return (
    <div className="app-root">
      <div className="phone-frame">
        {route === "home" ? <HomePage onNavigate={navigate} /> : null}
        {route === "record" ? (
          <SkinRecordPage
            record={record}
            isAnalyzing={isAnalyzing}
            onBack={goBack}
            onPhotoChange={handlePhotoChange}
            onRecordChange={handleRecordChange}
            onAnalyze={handleAnalyze}
          />
        ) : null}
        {route === "analysis" ? (
          <AnalysisPage analysis={analysis} onBack={goBack} onNavigate={navigate} />
        ) : null}
        {route === "routine" ? (
          <RoutinePage routine={routine} onBack={goBack} onNavigate={navigate} />
        ) : null}
        {route === "complete" ? (
          <RecordCompletePage
            routine={routine}
            isSaving={isSaving}
            onBack={goBack}
            onSave={handleSaveRoutine}
            onNavigate={navigate}
          />
        ) : null}
        {route === "report" ? <ReportPage report={report} onBack={goBack} onNavigate={navigate} /> : null}
        <BottomNavigation active={activeNav} onNavigate={navigate} />
      </div>
    </div>
  );
}
