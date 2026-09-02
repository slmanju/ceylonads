import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CategorySelector } from "./CategorySelector";
import type { CategoryResponse } from "../../types/api";

function category(overrides: Partial<CategoryResponse>): CategoryResponse {
  return { id: 0, name: "", slug: "", parentId: null, displayOrder: 0, active: true, ...overrides };
}

const categories: CategoryResponse[] = [
  category({ id: 1, name: "Vehicles", slug: "vehicles", parentId: null, displayOrder: 0 }),
  category({ id: 2, name: "Property", slug: "property", parentId: null, displayOrder: 1 }),
  category({ id: 3, name: "Cars", slug: "cars", parentId: 1, displayOrder: 0 }),
  category({ id: 4, name: "Motorcycles", slug: "motorcycles", parentId: 1, displayOrder: 1 }),
  category({ id: 5, name: "Sedans", slug: "sedans", parentId: 3, displayOrder: 0 }),
];

describe("CategorySelector", () => {
  it("shows root categories with an All Categories option", () => {
    render(<CategorySelector categories={categories} value="" onSelect={vi.fn()} onClose={vi.fn()} />);
    expect(screen.getByText("All Categories")).toBeInTheDocument();
    expect(screen.getByText("Vehicles")).toBeInTheDocument();
    expect(screen.getByText("Property")).toBeInTheDocument();
  });

  it("drills into a parent category with children instead of selecting it", async () => {
    const onSelect = vi.fn();
    render(<CategorySelector categories={categories} value="" onSelect={onSelect} onClose={vi.fn()} />);

    await userEvent.click(screen.getByText("Vehicles"));

    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.getByText("All Vehicles")).toBeInTheDocument();
    expect(screen.getByText("Cars")).toBeInTheDocument();
    expect(screen.getByText("Motorcycles")).toBeInTheDocument();
  });

  it("selecting a leaf category calls onSelect with its slug", async () => {
    const onSelect = vi.fn();
    render(<CategorySelector categories={categories} value="" onSelect={onSelect} onClose={vi.fn()} />);

    await userEvent.click(screen.getByText("Vehicles"));
    await userEvent.click(screen.getByText("Motorcycles"));

    expect(onSelect).toHaveBeenCalledWith("motorcycles");
  });

  it("selecting 'All X' selects the parent category itself", async () => {
    const onSelect = vi.fn();
    render(<CategorySelector categories={categories} value="" onSelect={onSelect} onClose={vi.fn()} />);

    await userEvent.click(screen.getByText("Vehicles"));
    await userEvent.click(screen.getByText("All Vehicles"));

    expect(onSelect).toHaveBeenCalledWith("vehicles");
  });

  it("supports arbitrary depth and Back navigation", async () => {
    const onClose = vi.fn();
    render(<CategorySelector categories={categories} value="" onSelect={vi.fn()} onClose={onClose} />);

    await userEvent.click(screen.getByText("Vehicles"));
    await userEvent.click(screen.getByText("Cars"));
    expect(screen.getByText("All Cars")).toBeInTheDocument();
    expect(screen.getByText("Sedans")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Back"));
    expect(screen.getByText("All Vehicles")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Back"));
    expect(screen.getByText("All Categories")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Back to filters"));
    expect(onClose).toHaveBeenCalled();
  });
});
