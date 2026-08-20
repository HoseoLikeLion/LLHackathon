function pointsFor(trends, key) {
  if (trends.length < 2) return "";
  const values = trends.map((item) => item[key]);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  return trends
    .map((item, index) => {
      const x = 22 + (254 * index) / (trends.length - 1);
      const y = 116 - ((item[key] - min) / range) * 72;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");
}

export default function ReportChart({ trends }) {
  const rednessPoints = pointsFor(trends, "redness");
  const moisturePoints = pointsFor(trends, "moisture");

  return (
    <div className="report-chart" aria-label="최근 피부 변화 그래프">
      <svg viewBox="0 0 300 150" role="img">
        <title>서버에서 불러온 최근 피부 변화 추이</title>
        <path className="chart-grid" d="M20 120H280" />
        <polyline className="chart-line chart-line--red" points={rednessPoints} />
        <polyline className="chart-line chart-line--blue" points={moisturePoints} />
        {rednessPoints.split(" ").filter(Boolean).map((point) => {
          const [cx, cy] = point.split(",");
          return <circle key={`red-${point}`} className="chart-dot chart-dot--red" cx={cx} cy={cy} r="4" />;
        })}
        {moisturePoints.split(" ").filter(Boolean).map((point) => {
          const [cx, cy] = point.split(",");
          return <circle key={`blue-${point}`} className="chart-dot chart-dot--blue" cx={cx} cy={cy} r="4" />;
        })}
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
