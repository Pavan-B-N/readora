export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface RegisterResponse {
  userId: string;
  email: string;
  createdAt: string;
}

export interface AccessTokenClaims {
  sub: string;
  email: string;
  roles: string[];
  iat: number;
  exp: number;
}
