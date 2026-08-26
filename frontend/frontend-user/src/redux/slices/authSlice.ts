import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { jwtDecode } from 'jwt-decode';
import { login as loginRequest, register as registerRequest } from '@/api/authApi';
import { extractErrorMessage } from '@/api/client';
import type { AccessTokenClaims, LoginRequest, LoginResponse, RegisterRequest } from '@/types/auth';

const REFRESH_TOKEN_KEY = 'readora_refresh_token';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: string | null;
  email: string | null;
  status: 'idle' | 'loading' | 'failed';
  error: string | null;
}

function readStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

function claimsFromToken(accessToken: string): { userId: string; email: string } {
  const claims = jwtDecode<AccessTokenClaims>(accessToken);
  return { userId: claims.sub, email: claims.email };
}

const initialState: AuthState = {
  accessToken: null,
  refreshToken: readStoredRefreshToken(),
  userId: null,
  email: null,
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

export const register = createAsyncThunk<LoginResponse, RegisterRequest, { rejectValue: string }>(
  'auth/register',
  async (request, { rejectWithValue }) => {
    try {
      await registerRequest(request);
      return await loginRequest({ email: request.email, password: request.password });
    } catch (error) {
      return rejectWithValue(extractErrorMessage(error, 'Registration failed'));
    }
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    tokensReceived(state, action: { payload: { accessToken: string; refreshToken: string } }) {
      const { accessToken, refreshToken } = action.payload;
      const { userId, email } = claimsFromToken(accessToken);

      state.accessToken = accessToken;
      state.refreshToken = refreshToken;
      state.userId = userId;
      state.email = email;

      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    },
    loggedOut(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.userId = null;
      state.email = null;

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
        const { userId, email } = claimsFromToken(accessToken);

        state.status = 'idle';
        state.accessToken = accessToken;
        state.refreshToken = refreshToken;
        state.userId = userId;
        state.email = email;

        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? action.error.message ?? 'Login failed';
      })
      .addCase(register.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(register.fulfilled, (state, action) => {
        const { accessToken, refreshToken } = action.payload;
        const { userId, email } = claimsFromToken(accessToken);

        state.status = 'idle';
        state.accessToken = accessToken;
        state.refreshToken = refreshToken;
        state.userId = userId;
        state.email = email;

        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      })
      .addCase(register.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? action.error.message ?? 'Registration failed';
      });
  },
});

export const { tokensReceived, loggedOut } = authSlice.actions;
export default authSlice.reducer;
