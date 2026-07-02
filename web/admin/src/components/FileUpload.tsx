import { useState } from 'react';
import { Button, Upload, type UploadProps } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { fileApi, type FileBizType, type FileAccessLevel } from '@/api/fileApi';
import { feedback } from '@/utils/feedback';

const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp';

export interface FileUploadProps {
  bizType: FileBizType;
  accessLevel?: FileAccessLevel;
  accept?: string;
  maxSizeMb?: number;
  disabled?: boolean;
  buttonText?: string;
  onUploaded?: (url: string) => void;
}

/** 通用单文件上传（服务端中转，成功后回调公开/受保护 URL）。 */
export function FileUpload({
  bizType,
  accessLevel,
  accept,
  maxSizeMb = 5,
  disabled,
  buttonText = '上传文件',
  onUploaded,
}: FileUploadProps) {
  const [uploading, setUploading] = useState(false);

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    const limit = maxSizeMb * 1024 * 1024;
    if (file.size > limit) {
      feedback.error(`文件大小不能超过 ${maxSizeMb}MB`);
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  const customRequest: UploadProps['customRequest'] = async (options) => {
    const { file, onSuccess, onError } = options;
    setUploading(true);
    try {
      const result = await fileApi.upload(file as File, { bizType, accessLevel, silent: true });
      feedback.success('上传成功');
      onUploaded?.(result.url);
      onSuccess?.(result);
    } catch (error) {
      onError?.(error as Error);
    } finally {
      setUploading(false);
    }
  };

  return (
    <Upload
      accept={accept ?? (bizType === 'avatar' || bizType === 'image' ? IMAGE_ACCEPT : undefined)}
      showUploadList={false}
      maxCount={1}
      disabled={disabled || uploading}
      beforeUpload={beforeUpload}
      customRequest={customRequest}
    >
      <Button icon={<UploadOutlined />} loading={uploading} disabled={disabled}>
        {buttonText}
      </Button>
    </Upload>
  );
}
