/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_PROXY_TARGET: string;
  readonly VITE_TOKEN_STORAGE: 'local' | 'session';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
