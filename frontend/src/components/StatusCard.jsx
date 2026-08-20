import { ArrowDown, ArrowRight, ArrowUp, Droplet, Leaf, Thermometer } from "lucide-react";

const toneIcons = {
  red: Thermometer,
  blue: Droplet,
  green: Leaf,
};

const trendIcons = {
  up: ArrowUp,
  down: ArrowDown,
  same: ArrowRight,
};

export default function StatusCard({ label, value, tone, trend }) {
  const Icon = toneIcons[tone] ?? Droplet;
  const TrendIcon = trendIcons[trend] ?? ArrowRight;

  return (
    <article className={`status-card status-card--${tone}`}>
      <div className="status-card__icon">
        <Icon size={22} strokeWidth={2.3} />
      </div>
      <span>{label}</span>
      <strong>{value}</strong>
      <TrendIcon className="status-card__trend" size={22} strokeWidth={2.2} />
    </article>
  );
}

