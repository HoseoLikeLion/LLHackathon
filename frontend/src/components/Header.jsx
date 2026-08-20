import { ArrowLeft, UserCircle } from "lucide-react";

export default function Header({
  title = "SkinAI",
  onBack,
  showBack = false,
  showProfile = true,
  align = "center",
}) {
  return (
    <header className="app-header">
      <div className="header-side">
        {showBack ? (
          <button className="icon-button" type="button" aria-label="뒤로 가기" onClick={onBack}>
            <ArrowLeft size={22} strokeWidth={2.2} />
          </button>
        ) : null}
      </div>
      <strong className={`header-title header-title--${align}`}>{title}</strong>
      <div className="header-side header-side--right">
        {showProfile ? (
          <span className="profile-icon" aria-label="프로필">
            <UserCircle size={22} strokeWidth={2.2} />
          </span>
        ) : null}
      </div>
    </header>
  );
}

