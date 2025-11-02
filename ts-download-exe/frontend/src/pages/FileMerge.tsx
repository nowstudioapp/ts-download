import React, { useState, useRef } from 'react';
import FileList from '../components/FileList';
import MergeOptions from '../components/MergeOptions';
import ProgressBar from '../components/ProgressBar';
import { MergeExcelFiles, SelectExcelFiles } from '../../wailsjs/go/main/App';
import { EventsOn } from '../../wailsjs/runtime';
import './FileMerge.css';

export interface FileItem {
  id: string;
  name: string;
  path: string;
}

export interface MergeConfig {
  removeDuplicates: boolean;
  deduplicateColumn: string;
  outputPath: string;
}

const FileMerge: React.FC = () => {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [progress, setProgress] = useState(0);
  const [isProcessing, setIsProcessing] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const addLog = (message: string) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${message}`]);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    const excelFiles = droppedFiles.filter(file => 
      file.name.endsWith('.xlsx') || file.name.endsWith('.xls') || file.name.endsWith('.csv')
    );

    if (excelFiles.length === 0) {
      addLog('⚠️ 未找到支持的文件 (支持 Excel 和 CSV 文件)');
      return;
    }

    const newFiles: FileItem[] = excelFiles.map(file => {
      // 在 Wails 中，尝试获取完整路径
      const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
      console.log('拖拽文件路径:', fullPath);
      
      return {
        id: `${Date.now()}-${Math.random()}`,
        name: file.name,
        path: fullPath,
      };
    });

    setFiles(prev => [...prev, ...newFiles]);
    addLog(`✓ 添加了 ${newFiles.length} 个文件`);
  };

  const handleFileSelect = async () => {
    try {
      const selectedPaths = await SelectExcelFiles();
      if (selectedPaths && selectedPaths.length > 0) {
        const newFiles: FileItem[] = selectedPaths.map(path => ({
          id: `${Date.now()}-${Math.random()}`,
          name: path.split('\\').pop() || path.split('/').pop() || path,
          path: path,
        }));
        
        setFiles(prev => [...prev, ...newFiles]);
        addLog(`✓ 添加了 ${newFiles.length} 个文件`);
      }
    } catch (error) {
      addLog(`❌ 选择文件失败: ${error}`);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    const supportedFiles = selectedFiles.filter(file => 
      file.name.endsWith('.xlsx') || file.name.endsWith('.xls') || file.name.endsWith('.csv')
    );

    if (supportedFiles.length === 0) {
      addLog('⚠️ 未找到支持的文件 (支持 Excel 和 CSV 文件)');
      return;
    }

    const newFiles: FileItem[] = supportedFiles.map(file => {
      // 在 Wails 中，尝试获取完整路径
      const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
      console.log('选择文件路径:', fullPath);
      
      return {
        id: `${Date.now()}-${Math.random()}`,
        name: file.name,
        path: fullPath,
      };
    });

    setFiles(prev => [...prev, ...newFiles]);
    addLog(`✓ 添加了 ${newFiles.length} 个文件`);
  };

  const handleRemoveFile = (id: string) => {
    setFiles(prev => prev.filter(f => f.id !== id));
    addLog('✓ 移除了 1 个文件');
  };

  const handleClearFiles = () => {
    setFiles([]);
    addLog('✓ 清空了所有文件');
  };

  const handleStartMerge = async (config: MergeConfig) => {
    if (files.length === 0) {
      addLog('❌ 请先添加文件');
      return;
    }

    if (!config.outputPath) {
      addLog('❌ 请选择输出位置');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    setLogs([]);
    addLog('开始合并文件...');

    try {
      const filePaths = files.map(f => f.path);
      setProgress(30);
      addLog(`正在读取 ${files.length} 个文件...`);

      const result = await MergeExcelFiles(filePaths, {
        removeDuplicates: config.removeDuplicates,
        deduplicateColumn: config.deduplicateColumn,
        outputPath: config.outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog(`✓ 合并完成！`);
        addLog(`  处理行数: ${result.rowsProcessed}`);
        addLog(`  去重后行数: ${result.rowsAfterDedupe}`);
        addLog(`  输出文件: ${result.outputPath}`);
        setProgress(100);
      } else {
        addLog(`❌ 合并失败: ${result.message}`);
        setProgress(0);
      }
    } catch (error) {
      addLog(`❌ 合并失败: ${error}`);
      setProgress(0);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="file-merge">
      <div className="page-header">
        <h2>📋 文件合并</h2>
        <p className="page-description">拖拽文件到下方区域或点击选择多个 Excel/CSV 文件进行合并，输出为 Excel 格式</p>
      </div>

      <div className="merge-container">
        <div className="left-panel">
          <div
            className={`drop-zone ${isDragging ? 'dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <div className="drop-zone-content">
              <span className="drop-icon">📁</span>
              <p className="drop-text">拖拽文件到此处或点击选择</p>
              <button className="select-button" onClick={handleFileSelect}>
                点击可连续选择多个文件
              </button>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept=".xlsx,.xls,.csv"
                style={{ display: 'none' }}
                onChange={handleFileInputChange}
              />
            </div>
          </div>

          <FileList
            files={files}
            onRemoveFile={handleRemoveFile}
            onClearFiles={handleClearFiles}
          />
        </div>

        <div className="right-panel">
          <MergeOptions
            onStartMerge={handleStartMerge}
            isProcessing={isProcessing}
            fileCount={files.length}
          />

          <ProgressBar progress={progress} />

          <div className="log-panel">
            <div className="log-header">
              <span className="log-icon">📋</span>
              <span className="log-title">处理日志</span>
            </div>
            <div className="log-content">
              {logs.length === 0 ? (
                <div className="log-empty">等待开始处理...</div>
              ) : (
                logs.map((log, index) => (
                  <div key={index} className="log-item">{log}</div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FileMerge;
