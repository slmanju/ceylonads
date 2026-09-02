import "./Carousel.css";

interface CarouselDotsProps {
  count: number;
  activeIndex: number;
  onSelect: (index: number) => void;
  labelPrefix: string;
}

export function CarouselDots({ count, activeIndex, onSelect, labelPrefix }: CarouselDotsProps) {
  if (count <= 1) return null;

  return (
    <div className="carousel-dots" role="tablist" aria-label={`${labelPrefix} pagination`}>
      {Array.from({ length: count }).map((_, index) => (
        <button
          key={index}
          type="button"
          role="tab"
          className={`carousel-dots__dot ${index === activeIndex ? "carousel-dots__dot--active" : ""}`}
          aria-label={`Go to ${labelPrefix.toLowerCase()} ${index + 1}`}
          aria-current={index === activeIndex}
          aria-selected={index === activeIndex}
          onClick={() => onSelect(index)}
        />
      ))}
    </div>
  );
}
