import React, { useLayoutEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

const preferenceKey = "iitsm-theme";

export function useTheme() {
  const [dark, setDark] = useState(() => {
    try {
      const saved = localStorage.getItem(preferenceKey);
      if (saved === "dark" || saved === "light") return saved === "dark";
    } catch {
      // Storage may be disabled. The toggle still works for this session.
    }
    return window.matchMedia?.("(prefers-color-scheme: dark)")?.matches ?? false;
  });

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = dark ? "dark" : "light";
  }, [dark]);

  function toggle() {
    const next = !dark;
    setDark(next);
    try {
      localStorage.setItem(preferenceKey, next ? "dark" : "light");
    } catch {
      // The displayed theme does not depend on storage.
    }
  }

  return { dark, toggle };
}

export default function ThemeToggle({ dark, toggle }) {
  const Icon = dark ? Sun : Moon;
  return (
    <button type="button" className="icon-button theme-toggle"
      aria-label="Dark mode" aria-pressed={dark}
      title={dark ? "Switch to light mode" : "Switch to dark mode"}
      onClick={toggle}>
      <Icon size={19} aria-hidden="true" />
    </button>
  );
}
