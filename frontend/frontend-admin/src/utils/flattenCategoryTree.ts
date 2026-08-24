import type { CategoryNode } from '@/types/catalog';

export interface FlatCategory {
  id: string;
  label: string;
  name: string;
  depth: number;
}

/** Flattens the category tree into a list, prefixing nested names so hierarchy stays readable in a flat <select>. */
export function flattenCategoryTree(nodes: CategoryNode[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, label: `${'— '.repeat(depth)}${node.name}`, name: node.name, depth },
    ...flattenCategoryTree(node.children, depth + 1),
  ]);
}
