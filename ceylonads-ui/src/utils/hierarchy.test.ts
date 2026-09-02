import { describe, expect, it } from "vitest";
import { childrenOf, findBySlug, hasChildren, rootsOf } from "./hierarchy";

interface Node {
  id: number;
  parentId: number | null;
  slug: string;
}

const nodes: Node[] = [
  { id: 1, parentId: null, slug: "vehicles" },
  { id: 2, parentId: null, slug: "property" },
  { id: 3, parentId: 1, slug: "cars" },
  { id: 4, parentId: 1, slug: "motorcycles" },
  { id: 5, parentId: 3, slug: "sedans" },
];

describe("hierarchy", () => {
  it("rootsOf returns only top-level nodes", () => {
    expect(rootsOf(nodes).map((n) => n.slug)).toEqual(["vehicles", "property"]);
  });

  it("childrenOf returns direct children only", () => {
    expect(childrenOf(nodes, 1).map((n) => n.slug)).toEqual(["cars", "motorcycles"]);
    expect(childrenOf(nodes, 3).map((n) => n.slug)).toEqual(["sedans"]);
  });

  it("hasChildren reflects arbitrary depth", () => {
    expect(hasChildren(nodes, 1)).toBe(true);
    expect(hasChildren(nodes, 3)).toBe(true);
    expect(hasChildren(nodes, 5)).toBe(false);
    expect(hasChildren(nodes, 4)).toBe(false);
  });

  it("findBySlug locates a node by slug", () => {
    expect(findBySlug(nodes, "sedans")?.id).toBe(5);
    expect(findBySlug(nodes, "missing")).toBeUndefined();
  });
});
