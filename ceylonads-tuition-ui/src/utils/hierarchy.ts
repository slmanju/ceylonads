interface TreeNode {
  id: number;
  parentId: number | null;
}

export function rootsOf<T extends TreeNode>(items: T[]): T[] {
  return items.filter((item) => item.parentId === null);
}

export function childrenOf<T extends TreeNode>(items: T[], parentId: number): T[] {
  return items.filter((item) => item.parentId === parentId);
}

export function hasChildren<T extends TreeNode>(items: T[], id: number): boolean {
  return items.some((item) => item.parentId === id);
}

export function findBySlug<T extends { slug: string }>(items: T[], slug: string): T | undefined {
  return items.find((item) => item.slug === slug);
}
