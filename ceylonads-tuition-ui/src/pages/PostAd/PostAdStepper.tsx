import { FaCheck } from "react-icons/fa";
import { STEPS } from "./types";
import "./PostAdStepper.css";

interface PostAdStepperProps {
  currentIndex: number;
  maxReachedIndex: number;
  onStepClick: (index: number) => void;
}

export function PostAdStepper({ currentIndex, maxReachedIndex, onStepClick }: PostAdStepperProps) {
  return (
    <ol className="post-ad-stepper" aria-label="Post tuition ad steps">
      {STEPS.map((step, index) => {
        const isDone = index < currentIndex;
        const isCurrent = index === currentIndex;
        const isReachable = index <= maxReachedIndex;

        return (
          <li key={step.key} className="post-ad-stepper__item">
            <button
              type="button"
              className={`post-ad-stepper__button ${isCurrent ? "post-ad-stepper__button--current" : ""} ${
                isDone ? "post-ad-stepper__button--done" : ""
              }`}
              onClick={() => isReachable && onStepClick(index)}
              disabled={!isReachable}
              aria-current={isCurrent ? "step" : undefined}
            >
              <span className="post-ad-stepper__dot">{isDone ? <FaCheck aria-hidden="true" /> : index + 1}</span>
              <span className="post-ad-stepper__label">{step.label}</span>
            </button>
            {index < STEPS.length - 1 && <span className="post-ad-stepper__connector" aria-hidden="true" />}
          </li>
        );
      })}
    </ol>
  );
}
