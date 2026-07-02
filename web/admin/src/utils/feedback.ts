import type { MessageInstance } from 'antd/es/message/interface';
import type { ModalStaticFunctions } from 'antd/es/modal/confirm';

/**
 * 全局反馈句柄。antd v5 推荐用 App.useApp() 拿到带主题上下文的实例，
 * 这里在根组件挂载时注入，供非组件代码（如 axios 拦截器）调用。
 */
interface FeedbackHandles {
  message: MessageInstance | null;
  modal: Omit<ModalStaticFunctions, 'warn'> | null;
}

const handles: FeedbackHandles = { message: null, modal: null };

export function bindFeedback(message: MessageInstance, modal: Omit<ModalStaticFunctions, 'warn'>): void {
  handles.message = message;
  handles.modal = modal;
}

export const feedback = {
  success(content: string): void {
    handles.message?.success(content);
  },
  error(content: string): void {
    handles.message?.error(content);
  },
  warning(content: string): void {
    handles.message?.warning(content);
  },
  info(content: string): void {
    handles.message?.info(content);
  },
  get modal() {
    return handles.modal;
  },
};
