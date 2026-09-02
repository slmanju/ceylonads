import type { LocationResponse } from "../../types/api";
import "./LocationTree.css";

interface LocationTreeProps {
  locations: LocationResponse[];
}

export function LocationTree({ locations }: LocationTreeProps) {
  const childrenOf = (parentId: number | null) =>
    locations.filter((l) => l.parentId === parentId).sort((a, b) => a.name.localeCompare(b.name));

  const renderNode = (location: LocationResponse, depth: number) => {
    const children = childrenOf(location.id);

    return (
      <li key={location.id} className="location-tree__item">
        <div className="location-tree__node" style={{ paddingLeft: depth * 20 }}>
          <span className="location-tree__name">{location.name}</span>
          <span className="location-tree__type">{location.type}</span>
        </div>
        {children.length > 0 && (
          <ul className="location-tree__children">{children.map((child) => renderNode(child, depth + 1))}</ul>
        )}
      </li>
    );
  };

  const roots = childrenOf(null);

  return <ul className="location-tree">{roots.map((root) => renderNode(root, 0))}</ul>;
}
