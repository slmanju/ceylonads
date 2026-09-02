import { useEffect, useRef, useState } from "react";
import type { MouseEvent, TouchEvent } from "react";
import { FaChevronLeft, FaChevronRight, FaSearchMinus, FaSearchPlus, FaTimes, FaUndo } from "react-icons/fa";
import type { MediaResponse } from "../../types/api";
import { resolveMediaUrl } from "../../api/apiClient";
import "./ImageLightbox.css";

interface ImageLightboxProps {
  media: MediaResponse[];
  index: number;
  title: string;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}

interface Pan {
  x: number;
  y: number;
}

const MIN_ZOOM = 1;
const MAX_ZOOM = 4;
const ZOOM_STEP = 0.5;
const WHEEL_ZOOM_STEP = 0.2;
const SWIPE_THRESHOLD = 50;

function clampZoom(zoom: number) {
  return Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom));
}

export function ImageLightbox({ media, index, title, onIndexChange, onClose }: ImageLightboxProps) {
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState<Pan>({ x: 0, y: 0 });
  const [isInteracting, setIsInteracting] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const imgRef = useRef<HTMLImageElement>(null);
  const dragState = useRef<{ startX: number; startY: number; panX: number; panY: number } | null>(null);
  const pinchState = useRef<{ distance: number; zoom: number } | null>(null);
  const swipeState = useRef<{ startX: number } | null>(null);

  const hasMultiple = media.length > 1;
  const active = media[index] ?? media[0];

  const goToPrev = () => {
    if (!hasMultiple) return;
    onIndexChange((index - 1 + media.length) % media.length);
  };

  const goToNext = () => {
    if (!hasMultiple) return;
    onIndexChange((index + 1) % media.length);
  };

  // Keeps the panned image from drifting past its own edges. Uses the image's laid-out (untransformed)
  // size, since a CSS transform doesn't affect clientWidth/clientHeight.
  const clampPan = (nextPan: Pan, nextZoom: number): Pan => {
    const container = containerRef.current;
    const img = imgRef.current;
    if (!container || !img || nextZoom <= 1) return { x: 0, y: 0 };
    const containerRect = container.getBoundingClientRect();
    const scaledWidth = img.clientWidth * nextZoom;
    const scaledHeight = img.clientHeight * nextZoom;
    const maxX = Math.max(0, (scaledWidth - containerRect.width) / 2);
    const maxY = Math.max(0, (scaledHeight - containerRect.height) / 2);
    return {
      x: Math.min(maxX, Math.max(-maxX, nextPan.x)),
      y: Math.min(maxY, Math.max(-maxY, nextPan.y)),
    };
  };

  const applyZoom = (nextZoom: number) => {
    const clamped = clampZoom(nextZoom);
    setZoom(clamped);
    setPan((prev) => (clamped <= 1 ? { x: 0, y: 0 } : clampPan(prev, clamped)));
  };

  const zoomIn = () => applyZoom(zoom + ZOOM_STEP);
  const zoomOut = () => applyZoom(zoom - ZOOM_STEP);
  const resetZoom = () => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  };
  const toggleZoom = () => (zoom > 1 ? resetZoom() : applyZoom(2));

  // Reset zoom/pan whenever the active image changes.
  useEffect(() => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  }, [index]);

  // Lock page scroll while the lightbox is open.
  useEffect(() => {
    const original = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = original;
    };
  }, []);

  // Reattached each render so handlers always see the latest zoom/index without stale closures.
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") goToPrev();
      else if (e.key === "ArrowRight") goToNext();
      else if (e.key === "Escape") onClose();
      else if (e.key === "+" || e.key === "=") zoomIn();
      else if (e.key === "-") zoomOut();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  });

  // Registered manually (not via onWheel) so preventDefault reliably stops page scroll while zooming.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const handleWheel = (e: WheelEvent) => {
      e.preventDefault();
      applyZoom(zoom + (e.deltaY < 0 ? WHEEL_ZOOM_STEP : -WHEEL_ZOOM_STEP));
    };
    container.addEventListener("wheel", handleWheel, { passive: false });
    return () => container.removeEventListener("wheel", handleWheel);
  });

  useEffect(() => {
    const handleMouseMove = (e: globalThis.MouseEvent) => {
      if (!dragState.current) return;
      const dx = e.clientX - dragState.current.startX;
      const dy = e.clientY - dragState.current.startY;
      setPan(clampPan({ x: dragState.current.panX + dx, y: dragState.current.panY + dy }, zoom));
    };
    const handleMouseUp = () => {
      dragState.current = null;
      setIsInteracting(false);
    };
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  });

  const handleMouseDown = (e: MouseEvent) => {
    if (zoom <= 1) return;
    dragState.current = { startX: e.clientX, startY: e.clientY, panX: pan.x, panY: pan.y };
    setIsInteracting(true);
  };

  const touchDistance = (touches: React.TouchList) => {
    const a = touches[0];
    const b = touches[1];
    return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
  };

  const handleTouchStart = (e: TouchEvent) => {
    if (e.touches.length === 2) {
      pinchState.current = { distance: touchDistance(e.touches), zoom };
      swipeState.current = null;
      setIsInteracting(true);
    } else if (e.touches.length === 1) {
      if (zoom > 1) {
        dragState.current = {
          startX: e.touches[0].clientX,
          startY: e.touches[0].clientY,
          panX: pan.x,
          panY: pan.y,
        };
        setIsInteracting(true);
      } else {
        swipeState.current = { startX: e.touches[0].clientX };
      }
    }
  };

  const handleTouchMove = (e: TouchEvent) => {
    if (e.touches.length === 2 && pinchState.current) {
      const distance = touchDistance(e.touches);
      applyZoom(pinchState.current.zoom * (distance / pinchState.current.distance));
    } else if (e.touches.length === 1 && dragState.current && zoom > 1) {
      const dx = e.touches[0].clientX - dragState.current.startX;
      const dy = e.touches[0].clientY - dragState.current.startY;
      setPan(clampPan({ x: dragState.current.panX + dx, y: dragState.current.panY + dy }, zoom));
    }
  };

  const handleTouchEnd = (e: TouchEvent) => {
    pinchState.current = null;
    dragState.current = null;
    setIsInteracting(false);
    if (swipeState.current && e.changedTouches.length === 1) {
      const delta = e.changedTouches[0].clientX - swipeState.current.startX;
      if (Math.abs(delta) > SWIPE_THRESHOLD) {
        if (delta > 0) goToPrev();
        else goToNext();
      }
    }
    swipeState.current = null;
  };

  return (
    <div className="image-lightbox" role="dialog" aria-modal="true" aria-label={`${title} image viewer`}>
      <div className="image-lightbox__toolbar">
        <span className="image-lightbox__counter">
          {index + 1} / {media.length}
        </span>
        <div className="image-lightbox__actions">
          <button type="button" onClick={zoomOut} disabled={zoom <= MIN_ZOOM} aria-label="Zoom out">
            <FaSearchMinus aria-hidden="true" />
          </button>
          <button type="button" onClick={resetZoom} aria-label="Reset zoom">
            <FaUndo aria-hidden="true" />
          </button>
          <button type="button" onClick={zoomIn} disabled={zoom >= MAX_ZOOM} aria-label="Zoom in">
            <FaSearchPlus aria-hidden="true" />
          </button>
          <button type="button" onClick={onClose} aria-label="Close">
            <FaTimes aria-hidden="true" />
          </button>
        </div>
      </div>

      <div
        className="image-lightbox__stage"
        ref={containerRef}
        onMouseDown={handleMouseDown}
        onDoubleClick={toggleZoom}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        <img
          ref={imgRef}
          src={resolveMediaUrl(active.url)}
          alt={title}
          className="image-lightbox__image"
          style={{
            transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
            transition: isInteracting ? "none" : "transform 0.15s ease",
            cursor: zoom > 1 ? "grab" : "zoom-in",
          }}
          draggable={false}
        />

        {hasMultiple && (
          <>
            <button
              type="button"
              className="image-lightbox__nav image-lightbox__nav--prev"
              onClick={goToPrev}
              aria-label="Previous image"
            >
              <FaChevronLeft aria-hidden="true" />
            </button>
            <button
              type="button"
              className="image-lightbox__nav image-lightbox__nav--next"
              onClick={goToNext}
              aria-label="Next image"
            >
              <FaChevronRight aria-hidden="true" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
