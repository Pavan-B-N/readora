import { useEffect, useRef, useState } from 'react';
import { useToast } from '@readora/shared-ui';
import { getCategoryTree, listAuthors } from '@/api/catalogApi';
import { extractErrorMessage } from '@/api/client';
import type { Author, CategoryNode } from '@/types/catalog';
import { useDebounced } from '@/hooks/useDebounced';

interface FlatCategory {
  id: string;
  name: string;
  depth: number;
}

function flatten(nodes: CategoryNode[], depth = 0): FlatCategory[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flatten(node.children, depth + 1),
  ]);
}

/** Category/author filter state for the home catalog, plus the filter-panel open/dismiss UI state. */
export function useCatalogFilters() {
  const { showToast } = useToast();

  const [categories, setCategories] = useState<FlatCategory[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [categoryId, setCategoryId] = useState('');
  const [authors, setAuthors] = useState<Author[]>([]);
  const [authorId, setAuthorId] = useState('');
  const [virtualOnly, setVirtualOnly] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const filtersRef = useRef<HTMLDivElement>(null);

  const debouncedCategoryId = useDebounced(categoryId, 150);

  useEffect(() => {
    getCategoryTree()
      .then((tree) => setCategories(flatten(tree)))
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load categories'), 'error'))
      .finally(() => setCategoriesLoading(false));

    listAuthors()
      .then(setAuthors)
      .catch((err) => showToast(extractErrorMessage(err, 'Could not load authors'), 'error'));
  }, [showToast]);

  useEffect(() => {
    if (!filtersOpen) return;
    const onClickOutside = (e: MouseEvent) => {
      if (filtersRef.current && !filtersRef.current.contains(e.target as Node)) setFiltersOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [filtersOpen]);

  const activeCategoryName = categories.find((c) => c.id === categoryId)?.name;
  const activeAuthorName = authors.find((a) => a.id === authorId)?.name;
  const activeFilterCount = (authorId ? 1 : 0) + (virtualOnly ? 1 : 0);

  const clearAll = () => {
    setCategoryId('');
    setAuthorId('');
    setVirtualOnly(false);
  };

  return {
    categories,
    categoriesLoading,
    categoryId,
    setCategoryId,
    debouncedCategoryId,
    authors,
    authorId,
    setAuthorId,
    virtualOnly,
    setVirtualOnly,
    filtersOpen,
    setFiltersOpen,
    filtersRef,
    activeCategoryName,
    activeAuthorName,
    activeFilterCount,
    clearAll,
  };
}
