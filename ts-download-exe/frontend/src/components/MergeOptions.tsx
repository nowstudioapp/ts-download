import React, { useState } from 'react';
import { MergeConfig } from '../pages/FileMerge';
import { SelectSaveFile } from '../../wailsjs/go/main/App';
import './MergeOptions.css';

interface MergeOptionsProps {
  onStartMerge: (config: MergeConfig) => void;
  isProcessing: boolean;
  fileCount: number;
}

const MergeOptions: React.FC<MergeOptionsProps> = ({ onStartMerge, isProcessing, fileCount }) => {
  const [removeDuplicates, setRemoveDuplicates] = useState(false);
  const [deduplicateColumn, setDeduplicateColumn] = useState('手机号码');
  const [outputPath, setOutputPath] = useState('');

  const handleSelectOutput = async () => {
    try {
      console.log('点击浏览按钮');
      console.log('SelectSaveFile 函数:', SelectSaveFile);
      
      const path = await SelectSaveFile();
      console.log('返回的路径:', path);
      
      if (path && path.trim()) {
        setOutputPath(path.trim());
        console.log('设置路径成功:', path.trim());
      } else {
        console.log('用户取消了选择');
      }
    } catch (error) {
      console.error('文件选择失败:', error);
      alert('文件对话框打开失败: ' + error);
      
      // 降级方案
      const path = prompt('请输入输出文件完整路径\n\n例如: C:\\Users\\你的用户名\\Desktop\\merged.xlsx');
      if (path && path.trim()) {
        setOutputPath(path.trim());
      }
    }
  };

  const handleStartMerge = () => {
    const config: MergeConfig = {
      removeDuplicates,
      deduplicateColumn: removeDuplicates ? deduplicateColumn : '',
      outputPath
    };
    onStartMerge(config);
  };

  return (
    <div className="merge-options">
      <div className="options-header">
        <span className="options-icon">⚙️</span>
        <span className="options-title">合并选项</span>
      </div>

      <div className="options-content">
        <div className="option-group">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={removeDuplicates}
              onChange={(e) => setRemoveDuplicates(e.target.checked)}
              disabled={isProcessing}
            />
            <span>去除重复行</span>
          </label>
        </div>

        {removeDuplicates && (
          <div className="option-group">
            <label className="input-label">
              去重列名:
              <input
                type="text"
                className="text-input"
                value={deduplicateColumn}
                onChange={(e) => setDeduplicateColumn(e.target.value)}
                placeholder="例如: 手机号码"
                disabled={isProcessing}
              />
            </label>
            <p className="input-hint">输入用于去重的列名（区分大小写）</p>
          </div>
        )}

        <div className="option-group">
          <label className="input-label">
            输出文件:
            <div className="output-path-group">
              <input
                type="text"
                className="text-input"
                value={outputPath}
                onChange={(e) => setOutputPath(e.target.value)}
                placeholder="例如: C:\\output\\merged.xlsx"
                disabled={isProcessing}
              />
              <button
                className="browse-button"
                onClick={handleSelectOutput}
                disabled={isProcessing}
              >
                浏览
              </button>
            </div>
          </label>
          <p className="input-hint">指定合并后的输出文件路径</p>
        </div>

        <button
          className="start-button"
          onClick={handleStartMerge}
          disabled={isProcessing || fileCount === 0 || !outputPath}
        >
          {isProcessing ? (
            <>
              <span className="spinner">⏳</span>
              <span>处理中...</span>
            </>
          ) : (
            <>
              <span>🚀</span>
              <span>开始合并</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};

export default MergeOptions;
