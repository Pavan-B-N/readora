import type { CategoryNode } from '@/types/catalog';

export interface FlatCategory {
  id: string;
  label: string;
}

export function flattenCategoryTree(nodes: CategoryNode[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, label: `${'— '.repeat(depth)}${node.name}` },
    ...flattenCategoryTree(node.children, depth + 1),
  ]);
}
