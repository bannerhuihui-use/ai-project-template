import { request } from './request';
import type { FileUploadResult } from '@/types/api';

export type FileBizType = 'avatar' | 'image' | 'document' | 'attachment';
export type FileAccessLevel = 'public' | 'private';

export interface FileUploadOptions {
  bizType: FileBizType;
  accessLevel?: FileAccessLevel;
  /** 静默错误，由组件自行提示 */
  silent?: boolean;
}

export const fileApi = {
  upload(file: File, options: FileUploadOptions): Promise<FileUploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('bizType', options.bizType);
    if (options.accessLevel) {
      formData.append('accessLevel', options.accessLevel);
    }
    return request<FileUploadResult>({
      url: '/v1/files/upload',
      method: 'POST',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      silent: options.silent,
    });
  },

  remove(fileKey: string): Promise<void> {
    return request<void>({ url: `/v1/files/${fileKey}`, method: 'DELETE' });
  },
};
