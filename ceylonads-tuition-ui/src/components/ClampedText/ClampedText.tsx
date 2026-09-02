import { useLayoutEffect, useRef, useState } from "react";
import "./ClampedText.css";

interface ClampedTextProps {
  text: string;
  maxLines?: number;
}

export function ClampedText({ text, maxLines = 6 }: ClampedTextProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [expanded, setExpanded] = useState(false);
  const [overflowing, setOverflowing] = useState(false);
  const [collapsedHeight, setCollapsedHeight] = useState<number>();

  const paragraphs = text
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean);

  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;

    const measure = () => {
      const lineHeight = parseFloat(getComputedStyle(el).lineHeight);
      const maxHeight = lineHeight * maxLines;
      setCollapsedHeight(maxHeight);
      setOverflowing(el.scrollHeight > maxHeight + 1);
    };
    measure();

    window.addEventListener("resize", measure);
    return () => window.removeEventListener("resize", measure);
  }, [text, maxLines]);

  return (
    <div className="clamped-text">
      <div
        ref={ref}
        className="clamped-text__content"
        style={!expanded && collapsedHeight ? { maxHeight: collapsedHeight, overflow: "hidden" } : undefined}
      >
        {paragraphs.map((paragraph, index) => (
          <p key={index}>{paragraph}</p>
        ))}
      </div>
      {overflowing && (
        <button type="button" className="clamped-text__toggle" onClick={() => setExpanded((v) => !v)}>
          {expanded ? "Show less" : "Show more"}
        </button>
      )}
    </div>
  );
}
