export default function SkinStatusSelector({ title, icon, options, value, onChange, tone }) {
  return (
    <section className="selector-card">
      <div className={`selector-card__title selector-card__title--${tone}`}>
        {icon}
        <strong>{title}</strong>
      </div>
      <div className="selector-card__options" role="radiogroup" aria-label={title}>
        {options.map((option) => (
          <button
            key={option}
            className={`choice-button${value === option ? " is-selected" : ""}`}
            type="button"
            role="radio"
            aria-checked={value === option}
            onClick={() => onChange(option)}
          >
            {option}
          </button>
        ))}
      </div>
    </section>
  );
}

