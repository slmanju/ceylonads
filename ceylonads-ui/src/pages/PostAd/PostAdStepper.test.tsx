import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { PostAdStepper } from "./PostAdStepper";

const DONE = "post-ad-stepper__button--done";
const CURRENT = "post-ad-stepper__button--current";

describe("PostAdStepper", () => {
  it("marks the current step active and every later step inactive", () => {
    render(<PostAdStepper currentIndex={0} maxReachedIndex={0} onStepClick={vi.fn()} />);

    const category = screen.getByRole("button", { name: /Category/ });
    expect(category).toHaveClass(CURRENT);
    expect(category).not.toHaveClass(DONE);

    for (const label of [/Details/, /Location/, /Photos/, /Review/]) {
      const step = screen.getByRole("button", { name: label });
      expect(step).not.toHaveClass(CURRENT);
      expect(step).not.toHaveClass(DONE);
    }
  });

  it("marks steps before the current one as done", () => {
    render(<PostAdStepper currentIndex={2} maxReachedIndex={2} onStepClick={vi.fn()} />);

    expect(screen.getByRole("button", { name: /Category/ })).toHaveClass(DONE);
    expect(screen.getByRole("button", { name: /Details/ })).toHaveClass(DONE);

    const location = screen.getByRole("button", { name: /Location/ });
    expect(location).toHaveClass(CURRENT);
    expect(location).not.toHaveClass(DONE);

    expect(screen.getByRole("button", { name: /Photos/ })).not.toHaveClass(DONE);
    expect(screen.getByRole("button", { name: /Review/ })).not.toHaveClass(DONE);
  });

  it("does not keep later steps marked done after navigating backward", () => {
    // User reached Review (maxReachedIndex 4), then clicked Back all the way to Category.
    render(<PostAdStepper currentIndex={0} maxReachedIndex={4} onStepClick={vi.fn()} />);

    const category = screen.getByRole("button", { name: /Category/ });
    expect(category).toHaveClass(CURRENT);
    expect(category).not.toHaveClass(DONE);

    for (const label of [/Details/, /Location/, /Photos/, /Review/]) {
      const step = screen.getByRole("button", { name: label });
      expect(step).not.toHaveClass(DONE);
      expect(step).not.toHaveClass(CURRENT);
    }
  });

  it("marks a mid-flow current step correctly after navigating backward from further along", () => {
    // User reached Review (maxReachedIndex 4), then clicked Back to Details.
    render(<PostAdStepper currentIndex={1} maxReachedIndex={4} onStepClick={vi.fn()} />);

    expect(screen.getByRole("button", { name: /Category/ })).toHaveClass(DONE);

    const details = screen.getByRole("button", { name: /Details/ });
    expect(details).toHaveClass(CURRENT);
    expect(details).not.toHaveClass(DONE);

    for (const label of [/Location/, /Photos/, /Review/]) {
      const step = screen.getByRole("button", { name: label });
      expect(step).not.toHaveClass(DONE);
      expect(step).not.toHaveClass(CURRENT);
    }
  });
});
