/** Credentials submitted to the login endpoint. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** The token pair and metadata returned by a successful login or refresh. */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/** The decoded payload of a Readora access token JWT. */
export interface AccessTokenClaims {
  sub: string;
  email: string;
  roles: string[];
  iat: number;
  exp: number;
}
