import { useDispatch, useSelector, type TypedUseSelectorHook } from 'react-redux';
import type { AppDispatch, RootState } from './store';

/** Typed version of react-redux's useDispatch, pre-bound to this app's AppDispatch. */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
/** Typed version of react-redux's useSelector, pre-bound to this app's RootState. */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
