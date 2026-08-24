import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { createCategory, getCategoryTree } from '@/api/catalogApi';
import type { CategoryNode } from '@/types/catalog';
import { flattenCategoryTree, type FlatCategory } from '@/utils/flattenCategoryTree';
import { useToast } from '@/components/Toast';
import { Card } from '@/components/Card';
import { Input, Select } from '@/components/Input';
import { Button } from '@/components/Button';
import styles from './CategoriesPage.module.css';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  slug: z.string().min(1, 'Slug is required'),
  parentId: z.string().optional(),
  displayOrder: z.string().regex(/^\d+$/, 'Must be a whole number, 0 or greater'),
});

type FormValues = z.infer<typeof schema>;

function CategoryTree({ nodes }: { nodes: CategoryNode[] }) {
  return (
    <ul className={styles.list}>
      {nodes.map((node) => (
        <li key={node.id}>
          {node.name}
          {node.children.length > 0 && <CategoryTree nodes={node.children} />}
        </li>
      ))}
    </ul>
  );
}

export function CategoriesPage() {
  const { showToast } = useToast();
  const [tree, setTree] = useState<CategoryNode[]>([]);
  const [flat, setFlat] = useState<FlatCategory[]>([]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { displayOrder: '0' } });

  const reload = () => {
    getCategoryTree().then((result) => {
      setTree(result);
      setFlat(flattenCategoryTree(result));
    });
  };

  useEffect(reload, []);

  const onSubmit = async (values: FormValues) => {
    try {
      await createCategory({
        name: values.name,
        slug: values.slug,
        parentId: values.parentId || null,
        displayOrder: Number(values.displayOrder),
      });
      showToast('Category created');
      reset({ name: '', slug: '', parentId: '', displayOrder: '0' });
      reload();
    } catch {
      showToast('Failed to create category', 'error');
    }
  };

  return (
    <div className={styles.page}>
      <div>
        <h1>Categories</h1>
        <Card>{tree.length === 0 ? <p>No categories yet.</p> : <CategoryTree nodes={tree} />}</Card>
      </div>

      <Card>
        <h2>New category</h2>
        <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
          <Input label="Name" error={errors.name?.message} {...register('name')} />
          <Input label="Slug" error={errors.slug?.message} {...register('slug')} />
          <Select label="Parent category" {...register('parentId')}>
            <option value="">— None (top-level) —</option>
            {flat.map((c) => (
              <option key={c.id} value={c.id}>
                {c.label}
              </option>
            ))}
          </Select>
          <Input label="Display order" type="number" error={errors.displayOrder?.message} {...register('displayOrder')} />
          <Button type="submit" disabled={isSubmitting}>
            Create category
          </Button>
        </form>
      </Card>
    </div>
  );
}
