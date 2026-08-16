/** Credentials submitted to POST /auth/login. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Token pair returned by a successful login or refresh. */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/** Fields submitted to create a new account. */
export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

/** Confirmation returned after account creation (login still has to be called separately for tokens). */
export interface RegisterResponse {
  userId: string;
  email: string;
  createdAt: string;
}

/** Decoded payload of the JWT access token. */
export interface AccessTokenClaims {
  sub: string;
  email: string;
  roles: string[];
  iat: number;
  exp: number;
}
