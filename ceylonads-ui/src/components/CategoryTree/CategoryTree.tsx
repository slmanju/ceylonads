import { Link } from "react-router-dom";
import type { CategoryResponse } from "../../types/api";
import "./CategoryTree.css";

interface CategoryTreeProps {
  categories: CategoryResponse[];
}

export function CategoryTree({ categories }: CategoryTreeProps) {
  const childrenOf = (parentId: number | null) =>
    categories.filter((c) => c.parentId === parentId).sort((a, b) => a.displayOrder - b.displayOrder);

  const renderNode = (category: CategoryResponse, depth: number) => {
    const children = childrenOf(category.id);

    return (
      <li key={category.id} className="category-tree__item">
        <div className="category-tree__node" style={{ paddingLeft: depth * 20 }}>
          <span className="category-tree__name">{category.name}</span>
          <span className="category-tree__slug">/{category.slug}</span>
          {!category.active && <span className="category-tree__inactive">Inactive</span>}
          <Link to={`/admin/categories/${category.id}/attributes`} className="category-tree__attributes-link">
            Manage attributes
          </Link>
        </div>
        {children.length > 0 && (
          <ul className="category-tree__children">{children.map((child) => renderNode(child, depth + 1))}</ul>
        )}
      </li>
    );
  };

  const roots = childrenOf(null);

  return <ul className="category-tree">{roots.map((root) => renderNode(root, 0))}</ul>;
}
