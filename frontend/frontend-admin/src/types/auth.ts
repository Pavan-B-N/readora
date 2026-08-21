/** Credentials submitted to the login endpoint. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Token pair and metadata returned by login/refresh. */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/** Decoded payload of the JWT access token. */
export interface AccessTokenClaims {
  sub: string;
  email: string;
  roles: string[];
  iat: number;
  exp: number;
}
