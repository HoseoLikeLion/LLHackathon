import { BarChart3, Home, NotebookPen } from "lucide-react";

const items = [
  { key: "home", label: "홈", icon: Home, route: "home" },
  { key: "record", label: "기록", icon: NotebookPen, route: "record" },
  { key: "report", label: "리포트", icon: BarChart3, route: "report" },
];

export default function BottomNavigation({ active, onNavigate }) {
  return (
    <nav className="bottom-nav" aria-label="하단 네비게이션">
      {items.map((item) => {
        const Icon = item.icon;
        const isActive = active === item.key;

        return (
          <button
            key={item.key}
            className={`bottom-nav__item${isActive ? " is-active" : ""}`}
            type="button"
            aria-current={isActive ? "page" : undefined}
            onClick={() => onNavigate(item.route)}
          >
            <Icon size={20} strokeWidth={2.2} />
            <span>{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

