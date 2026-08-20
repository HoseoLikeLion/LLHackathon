export default function Button({
  children,
  variant = "primary",
  full = true,
  className = "",
  ...props
}) {
  const classes = ["button", `button--${variant}`, full ? "button--full" : "", className]
    .filter(Boolean)
    .join(" ");

  return (
    <button className={classes} type="button" {...props}>
      {children}
    </button>
  );
}

