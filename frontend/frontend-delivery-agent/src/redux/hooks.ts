import { useDispatch, useSelector, type TypedUseSelectorHook } from 'react-redux';
import type { AppDispatch, RootState } from './store';

/** Typed useDispatch — dispatches thunks/actions with AppDispatch's type instead of a plain Dispatch. */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
/** Typed useSelector — selector callbacks get RootState instead of unknown/any. */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
