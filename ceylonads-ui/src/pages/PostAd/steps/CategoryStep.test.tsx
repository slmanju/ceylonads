import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CategoryStep } from "./CategoryStep";
import type { CategoryResponse } from "../../../types/api";

const categories: CategoryResponse[] = [
  { id: 1, name: "Vehicles", slug: "vehicles", parentId: null, displayOrder: 0, active: true },
  { id: 2, name: "Cars", slug: "cars", parentId: 1, displayOrder: 0, active: true },
  { id: 3, name: "Motorcycles", slug: "motorcycles", parentId: 1, displayOrder: 1, active: true },
  { id: 4, name: "Electronics", slug: "electronics", parentId: null, displayOrder: 1, active: true },
  { id: 5, name: "Mobile Phones", slug: "mobile-phones", parentId: 4, displayOrder: 0, active: true },
];

function renderStep(props: Partial<React.ComponentProps<typeof CategoryStep>> = {}) {
  const onSelect = vi.fn();
  render(
    <CategoryStep
      categories={categories}
      loading={false}
      error={null}
      categorySlug=""
      onSelect={onSelect}
      {...props}
    />,
  );
  return { onSelect };
}

describe("CategoryStep", () => {
  it("shows only top-level categories initially", () => {
    renderStep();
    expect(screen.getByText("Vehicles")).toBeInTheDocument();
    expect(screen.getByText("Electronics")).toBeInTheDocument();
    expect(screen.queryByText("Cars")).not.toBeInTheDocument();
    expect(screen.queryByText("Motorcycles")).not.toBeInTheDocument();
    expect(screen.queryByText("Mobile Phones")).not.toBeInTheDocument();
  });

  it("shows only the selected parent's children after drilling in", async () => {
    renderStep();
    await userEvent.click(screen.getByText("Vehicles"));

    expect(screen.getByText("Cars")).toBeInTheDocument();
    expect(screen.getByText("Motorcycles")).toBeInTheDocument();
    expect(screen.queryByText("Electronics")).not.toBeInTheDocument();
    expect(screen.queryByText("Mobile Phones")).not.toBeInTheDocument();
  });

  it("selects a leaf category when clicked", async () => {
    const { onSelect } = renderStep();
    await userEvent.click(screen.getByText("Vehicles"));
    await userEvent.click(screen.getByText("Cars"));

    expect(onSelect).toHaveBeenCalledWith("cars", "Vehicles › Cars");
  });

  it("finds and selects a leaf category via search without manual drill-down", async () => {
    const { onSelect } = renderStep();
    await userEvent.type(screen.getByPlaceholderText("Search category…"), "Motorcycles");

    expect(screen.getByText("Motorcycles")).toBeInTheDocument();
    expect(screen.getByText("Vehicles › Motorcycles")).toBeInTheDocument();
    await userEvent.click(screen.getByText("Motorcycles"));

    expect(onSelect).toHaveBeenCalledWith("motorcycles", "Vehicles › Motorcycles");
  });

  it("shows the selected category path", () => {
    renderStep({ categorySlug: "cars" });
    expect(screen.getByText((_, node) => node?.textContent === "Selected: Vehicles › Cars")).toBeInTheDocument();
  });
});
