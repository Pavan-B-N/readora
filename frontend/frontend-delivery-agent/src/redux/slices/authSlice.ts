import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { jwtDecode } from 'jwt-decode';
import { login as loginRequest } from '@/api/authApi';
import { extractErrorMessage } from '@/api/client';
import type { AccessTokenClaims, LoginRequest, LoginResponse } from '@/types/auth';

const REFRESH_TOKEN_KEY = 'readora_delivery_refresh_token';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  email: string | null;
  roles: string[];
  status: 'idle' | 'loading' | 'failed';
  error: string | null;
}

function readStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

function claimsFromToken(accessToken: string): { email: string; roles: string[] } {
  const claims = jwtDecode<AccessTokenClaims>(accessToken);
  return { email: claims.email, roles: claims.roles ?? [] };
}

const initialState: AuthState = {
  accessToken: null,
  refreshToken: readStoredRefreshToken(),
  email: null,
  roles: [],
  status: 'idle',
  error: null,
};

export const login = createAsyncThunk<LoginResponse, LoginRequest, { rejectValue: string }>(
  'auth/login',
  async (request, { rejectWithValue }) => {
    try {
      return await loginRequest(request);
    } catch (error) {
      return rejectWithValue(extractErrorMessage(error, 'Login failed'));
    }
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    tokensReceived(state, action: { payload: { accessToken: string; refreshToken: string } }) {
      const { accessToken, refreshToken } = action.payload;
      const { email, roles } = claimsFromToken(accessToken);

      state.accessToken = accessToken;
      state.refreshToken = refreshToken;
      state.email = email;
      state.roles = roles;

      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    },
    loggedOut(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.email = null;
      state.roles = [];

      localStorage.removeItem(REFRESH_TOKEN_KEY);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        const { accessToken, refreshToken } = action.payload;
        const { email, roles } = claimsFromToken(accessToken);

        state.status = 'idle';
        state.accessToken = accessToken;
        state.refreshToken = refreshToken;
        state.email = email;
        state.roles = roles;

        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? action.error.message ?? 'Login failed';
      });
  },
});

export const { tokensReceived, loggedOut } = authSlice.actions;
export default authSlice.reducer;
