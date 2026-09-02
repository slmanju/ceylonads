import { FaChevronLeft, FaChevronRight } from "react-icons/fa";
import "./Carousel.css";

interface CarouselArrowsProps {
  onPrev: () => void;
  onNext: () => void;
  labelPrefix: string;
}

export function CarouselArrows({ onPrev, onNext, labelPrefix }: CarouselArrowsProps) {
  return (
    <>
      <button
        type="button"
        className="carousel-arrow carousel-arrow--prev"
        aria-label={`Previous ${labelPrefix.toLowerCase()}`}
        onClick={onPrev}
      >
        <FaChevronLeft aria-hidden="true" />
      </button>
      <button
        type="button"
        className="carousel-arrow carousel-arrow--next"
        aria-label={`Next ${labelPrefix.toLowerCase()}`}
        onClick={onNext}
      >
        <FaChevronRight aria-hidden="true" />
      </button>
    </>
  );
}
