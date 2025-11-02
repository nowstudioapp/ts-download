import React from 'react';
import { FileItem } from '../pages/FileMerge';
import './FileList.css';

interface FileListProps {
  files: FileItem[];
  onRemoveFile: (id: string) => void;
  onClearFiles: () => void;
}

const FileList: React.FC<FileListProps> = ({ files, onRemoveFile, onClearFiles }) => {
  return (
    <div className="file-list">
      <div className="file-list-header">
        <span className="file-list-title">
          📄 已选择的文件: <span className="file-count">{files.length}</span>
        </span>
        {files.length > 0 && (
          <button className="clear-button" onClick={onClearFiles}>
            清空列表
          </button>
        )}
      </div>
      <div className="file-list-content">
        {files.length === 0 ? (
          <div className="file-list-empty">
            <p>暂无文件</p>
          </div>
        ) : (
          <div className="file-items">
            {files.map((file) => (
              <div key={file.id} className="file-item">
                <div className="file-info">
                  <span className="file-icon">📊</span>
                  <div className="file-details">
                    <div className="file-name" title={file.name}>{file.name}</div>
                  </div>
                </div>
                <button
                  className="remove-button"
                  onClick={() => onRemoveFile(file.id)}
                  title="移除文件"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default FileList;
