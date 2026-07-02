import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bindSessionGuard } from '@/auth/sessionGuard';
import { useSessionStore } from '@/store/sessionStore';

/** 将会话守卫绑定到 React Router（须在 BrowserRouter 内挂载）。 */
export default function SessionGuardHost() {
  const navigate = useNavigate();

  useEffect(() => {
    bindSessionGuard(navigate, () => useSessionStore.getState().reset());
  }, [navigate]);

  return null;
}
