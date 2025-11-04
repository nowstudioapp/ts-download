import React, { useState, useRef } from 'react';
import { SelectTxtFile, SelectSaveFile, ProcessTxtFiles } from '../../wailsjs/go/main/App';
import './TxtProcessor.css';

export interface TxtProcessConfig {
  mainFile: string;
  subFile: string;
  filterType: 'include' | 'exclude';
  outputPath: string;
}

const TxtProcessor: React.FC = () => {
  const [mainFile, setMainFile] = useState<string>('');
  const [subFile, setSubFile] = useState<string>('');
  const [filterType, setFilterType] = useState<'include' | 'exclude'>('include');
  const [outputPath, setOutputPath] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [isDraggingMain, setIsDraggingMain] = useState<boolean>(false);
  const [isDraggingSub, setIsDraggingSub] = useState<boolean>(false);

  const addLog = (message: string) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${message}`]);
  };

  const handleSelectMainFile = async () => {
    try {
      const selectedPath = await SelectTxtFile();
      if (selectedPath && selectedPath.trim()) {
        setMainFile(selectedPath.trim());
        addLog(`✓ 选择主TXT文件: ${selectedPath.split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择主TXT文件失败: ${error}`);
    }
  };

  const handleSelectSubFile = async () => {
    try {
      const selectedPath = await SelectTxtFile();
      if (selectedPath && selectedPath.trim()) {
        setSubFile(selectedPath.trim());
        addLog(`✓ 选择副TXT文件: ${selectedPath.split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择副TXT文件失败: ${error}`);
    }
  };

  const handleSelectOutput = async () => {
    try {
      const path = await SelectSaveFile();
      if (path && path.trim()) {
        // 确保输出文件是 .txt 格式
        let outputPath = path.trim();
        if (!outputPath.toLowerCase().endsWith('.txt')) {
          outputPath += '.txt';
        }
        setOutputPath(outputPath);
        addLog(`✓ 设置输出路径: ${outputPath.split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择输出路径失败: ${error}`);
    }
  };

  const handleStartProcess = async () => {
    if (!mainFile || !subFile || !outputPath) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    addLog('🚀 开始TXT文件处理...');

    try {
      setProgress(20);
      addLog(`📄 正在读取主TXT文件: ${mainFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`📄 正在读取副TXT文件: ${subFile.split('\\').pop()}`);
      
      setProgress(60);
      addLog(`🔍 正在执行${filterType === 'include' ? '包含' : '排除'}处理...`);
      
      const result = await ProcessTxtFiles({
        mainFile,
        subFile,
        filterType,
        outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog(`✅ 处理完成！`);
        addLog(`  处理行数: ${result.rowsProcessed}`);
        addLog(`  过滤行数: ${result.rowsFiltered}`);
        addLog(`  保留行数: ${result.rowsProcessed - result.rowsFiltered}`);
        addLog(`  输出文件: ${result.outputPath}`);
        setProgress(100);
      } else {
        addLog(`❌ 处理失败: ${result.message}`);
        setProgress(0);
      }
    } catch (error) {
      addLog(`❌ 处理失败: ${error}`);
      setProgress(0);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClearLogs = () => {
    setLogs([]);
  };

  // 主文件拖拽处理
  const handleMainDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingMain(true);
  };

  const handleMainDragLeave = () => {
    setIsDraggingMain(false);
  };

  const handleMainDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingMain(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    const txtFiles = droppedFiles.filter(file => 
      file.name.endsWith('.txt')
    );

    if (txtFiles.length === 0) {
      addLog('⚠️ 请拖拽 TXT 文件');
      return;
    }

    const file = txtFiles[0];
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽主TXT文件路径:', fullPath);
    
    setMainFile(fullPath);
    addLog(`✓ 拖拽主TXT文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
  };

  // 副文件拖拽处理
  const handleSubDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingSub(true);
  };

  const handleSubDragLeave = () => {
    setIsDraggingSub(false);
  };

  const handleSubDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingSub(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    const txtFiles = droppedFiles.filter(file => 
      file.name.endsWith('.txt')
    );

    if (txtFiles.length === 0) {
      addLog('⚠️ 请拖拽 TXT 文件');
      return;
    }

    const file = txtFiles[0];
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽副TXT文件路径:', fullPath);
    
    setSubFile(fullPath);
    addLog(`✓ 拖拽副TXT文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
  };

  return (
    <div className="txt-processor">
      <div className="processor-header">
        <h2>📝 TXT文件处理</h2>
        <p>根据副TXT文件内容，对主TXT文件进行包含或排除处理</p>
      </div>

      <div className="processor-content">
        <div className="upload-section">
          {/* 主TXT文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box main-upload ${isDraggingMain ? 'dragging' : ''}`}
              onClick={handleSelectMainFile}
              onDragOver={handleMainDragOver}
              onDragLeave={handleMainDragLeave}
              onDrop={handleMainDrop}
            >
              <div className="upload-icon">📄</div>
              <div className="upload-text">
                <div className="upload-title">选择主TXT文件</div>
                <div className="upload-subtitle">点击选择或拖拽主TXT文件到此处</div>
              </div>
            </div>
            {mainFile && (
              <div className="selected-file">
                <span className="file-icon">📄</span>
                <span className="file-name">{mainFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setMainFile('')}>✕</button>
              </div>
            )}
          </div>

          {/* 副TXT文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box sub-upload ${isDraggingSub ? 'dragging' : ''}`}
              onClick={handleSelectSubFile}
              onDragOver={handleSubDragOver}
              onDragLeave={handleSubDragLeave}
              onDrop={handleSubDrop}
            >
              <div className="upload-icon">📋</div>
              <div className="upload-text">
                <div className="upload-title">选择副TXT文件</div>
                <div className="upload-subtitle">点击选择或拖拽副TXT文件到此处</div>
              </div>
            </div>
            {subFile && (
              <div className="selected-file">
                <span className="file-icon">📋</span>
                <span className="file-name">{subFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setSubFile('')}>✕</button>
              </div>
            )}
          </div>
        </div>

        {/* 处理选项 */}
        <div className="process-options">
          <h3>🔧 处理选项</h3>
          
          <div className="option-group">
            <label className="input-label">处理模式:</label>
            <div className="radio-group">
              <label className="radio-option">
                <input
                  type="radio"
                  name="filterType"
                  value="include"
                  checked={filterType === 'include'}
                  onChange={(e) => setFilterType(e.target.value as 'include')}
                  disabled={isProcessing}
                />
                <span className="radio-text">包含 - 保留副文件中存在的行</span>
              </label>
              <label className="radio-option">
                <input
                  type="radio"
                  name="filterType"
                  value="exclude"
                  checked={filterType === 'exclude'}
                  onChange={(e) => setFilterType(e.target.value as 'exclude')}
                  disabled={isProcessing}
                />
                <span className="radio-text">排除 - 删除副文件中存在的行</span>
              </label>
            </div>
            <p className="input-hint">
              {filterType === 'include' 
                ? '保留主文件中在副文件中也存在的行' 
                : '删除主文件中在副文件中存在的行'
              }
            </p>
          </div>

          <div className="option-group">
            <label className="input-label">
              输出文件:
              <div className="output-path-group">
                <input
                  type="text"
                  className="text-input"
                  value={outputPath}
                  onChange={(e) => setOutputPath(e.target.value)}
                  placeholder="例如: C:\\output\\result.txt"
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
            <p className="input-hint">指定处理后的输出文件路径（.txt格式）</p>
          </div>

          <button
            className="start-button"
            onClick={handleStartProcess}
            disabled={isProcessing || !mainFile || !subFile || !outputPath}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                <span>处理中...</span>
              </>
            ) : (
              <>
                <span>🔄</span>
                <span>开始处理</span>
              </>
            )}
          </button>
        </div>

        {/* 进度条 */}
        {isProcessing && (
          <div className="progress-section">
            <h3>📊 进度状态</h3>
            <div className="progress-bar">
              <div className="progress-fill" style={{ width: `${progress}%` }}></div>
              <span className="progress-text">{progress}%</span>
            </div>
          </div>
        )}

        {/* 处理日志 */}
        <div className="log-section">
          <div className="log-header">
            <h3>📋 处理日志</h3>
            <button className="clear-button" onClick={handleClearLogs}>
              清空日志
            </button>
          </div>
          <div className="log-content">
            {logs.length === 0 ? (
              <div className="log-empty">暂无日志</div>
            ) : (
              logs.map((log, index) => (
                <div key={index} className="log-item">
                  {log}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default TxtProcessor;
