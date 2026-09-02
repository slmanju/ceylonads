import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { LocationSelector } from "./LocationSelector";
import type { LocationResponse } from "../../types/api";

function location(overrides: Partial<LocationResponse>): LocationResponse {
  return { id: 0, name: "", slug: "", type: "PROVINCE", parentId: null, ...overrides };
}

const locations: LocationResponse[] = [
  location({ id: 1, name: "Western Province", slug: "western", type: "PROVINCE", parentId: null }),
  location({ id: 2, name: "Colombo District", slug: "colombo-district", type: "DISTRICT", parentId: 1 }),
  location({ id: 3, name: "Colombo", slug: "colombo", type: "CITY", parentId: 2 }),
  location({ id: 4, name: "Kandy", slug: "kandy", type: "CITY", parentId: null }),
];

describe("LocationSelector", () => {
  it("defaults to All Sri Lanka at the root", () => {
    render(<LocationSelector locations={locations} value="" onSelect={vi.fn()} onClose={vi.fn()} />);
    expect(screen.getByText("All Sri Lanka")).toBeInTheDocument();
    expect(screen.getByText("Western Province")).toBeInTheDocument();
  });

  it("drills into a province and shows Back navigation", async () => {
    const onSelect = vi.fn();
    render(<LocationSelector locations={locations} value="" onSelect={onSelect} onClose={vi.fn()} />);

    await userEvent.click(screen.getByText("Western Province"));
    expect(onSelect).not.toHaveBeenCalled();
    expect(screen.getByText("All Western Province")).toBeInTheDocument();
    expect(screen.getByText("Colombo District")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Back"));
    expect(screen.getByText("All Sri Lanka")).toBeInTheDocument();
  });

  it("selecting a leaf location calls onSelect with its slug", async () => {
    const onSelect = vi.fn();
    render(<LocationSelector locations={locations} value="" onSelect={onSelect} onClose={vi.fn()} />);

    await userEvent.click(screen.getByText("Kandy"));
    expect(onSelect).toHaveBeenCalledWith("kandy");
  });

  it("searches across all levels regardless of current depth", async () => {
    const onSelect = vi.fn();
    render(<LocationSelector locations={locations} value="" onSelect={onSelect} onClose={vi.fn()} />);

    await userEvent.type(screen.getByPlaceholderText("Search location..."), "colombo");

    expect(screen.getByText("Colombo District")).toBeInTheDocument();
    expect(screen.getByText("Colombo")).toBeInTheDocument();
    expect(screen.queryByText("Kandy")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText("Colombo"));
    expect(onSelect).toHaveBeenCalledWith("colombo");
  });
});
