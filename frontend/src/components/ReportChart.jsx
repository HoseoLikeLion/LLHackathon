export default function ReportChart() {
  return (
    <div className="report-chart" aria-label="최근 피부 변화 그래프">
      <svg viewBox="0 0 300 150" role="img">
        <title>붉음은 낮아지고 수분은 개선되는 추이</title>
        <path className="chart-grid" d="M20 120H280" />
        <path className="chart-line chart-line--red" d="M22 98 C70 84 106 82 144 72 S218 44 276 64" />
        <path className="chart-line chart-line--blue" d="M22 116 C66 42 104 78 144 84 S222 126 276 56" />
        <circle className="chart-dot chart-dot--red" cx="22" cy="98" r="4" />
        <circle className="chart-dot chart-dot--red" cx="144" cy="72" r="4" />
        <circle className="chart-dot chart-dot--red" cx="276" cy="64" r="4" />
        <circle className="chart-dot chart-dot--blue" cx="22" cy="116" r="4" />
        <circle className="chart-dot chart-dot--blue" cx="144" cy="84" r="4" />
        <circle className="chart-dot chart-dot--blue" cx="276" cy="56" r="4" />
      </svg>
      <div className="chart-legend">
        <span>
          <i className="legend-dot legend-dot--red" />
          붉음
        </span>
        <span>
          <i className="legend-dot legend-dot--blue" />
          수분
        </span>
      </div>
    </div>
  );
}

