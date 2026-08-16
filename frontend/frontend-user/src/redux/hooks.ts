import { useDispatch, useSelector, type TypedUseSelectorHook } from 'react-redux';
import type { AppDispatch, RootState } from './store';

/** Pre-typed useDispatch — dispatch calls get AppDispatch's thunk typing without re-declaring it at every call site. */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
/** Pre-typed useSelector — selectors get RootState inference without re-declaring it at every call site. */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
