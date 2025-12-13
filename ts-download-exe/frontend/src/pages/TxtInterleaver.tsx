import React, { useState } from 'react';
import { SelectTxtFile, SelectSaveTxtFile, InterleaveTxtFiles } from '../../wailsjs/go/main/App';
import './TxtInterleaver.css';

export interface TxtInterleaveConfig {
  mainFile: string;
  subFile: string;
  outputPath: string;
}

const TxtInterleaver: React.FC = () => {
  const [mainFile, setMainFile] = useState<string>('');
  const [subFile, setSubFile] = useState<string>('');
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
      const path = await SelectSaveTxtFile();
      if (path && path.trim()) {
        setOutputPath(path.trim());
        addLog(`✓ 设置输出路径: ${path.split('\\').pop()}`);
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
    addLog('🚀 开始文本打散处理...');

    try {
      setProgress(20);
      addLog(`📄 正在读取主TXT文件: ${mainFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`📄 正在读取副TXT文件: ${subFile.split('\\').pop()}`);
      
      setProgress(60);
      addLog(`🔀 正在将副文件内容打散插入到主文件中...`);
      
      const result = await InterleaveTxtFiles({
        mainFile,
        subFile,
        outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog(`✅ 处理完成！`);
        addLog(`  主文件行数: ${result.mainLines}`);
        addLog(`  副文件行数: ${result.subLines}`);
        addLog(`  合并后总行数: ${result.totalLines}`);
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
    <div className="txt-interleaver">
      <div className="processor-header">
        <h2>🔀 文本打散合并</h2>
        <p>将副TXT文件的内容均匀打散插入到主TXT文件中，生成新的合并文件</p>
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
              <div className="selected-file main-file">
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
                <div className="upload-subtitle">点击选择或拖拽副TXT文件到此处（将被打散插入）</div>
              </div>
            </div>
            {subFile && (
              <div className="selected-file sub-file">
                <span className="file-icon">📋</span>
                <span className="file-name">{subFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setSubFile('')}>✕</button>
              </div>
            )}
          </div>
        </div>

        {/* 说明区域 */}
        <div className="info-section">
          <h3>📝 功能说明</h3>
          <div className="info-content">
            <div className="info-item">
              <span className="info-icon">📊</span>
              <span className="info-text">根据两个文件的行数比例，自动计算最佳插入间隔</span>
            </div>
            <div className="info-item">
              <span className="info-icon">🔄</span>
              <span className="info-text">例如：主文件2000行，副文件1000行 → 每2行主文件后插入1行副文件</span>
            </div>
            <div className="info-item">
              <span className="info-icon">⚖️</span>
              <span className="info-text">如果副文件比主文件多，则每行主文件后会插入多行副文件</span>
            </div>
            <div className="info-item">
              <span className="info-icon">✨</span>
              <span className="info-text">保证副文件内容均匀分布在最终结果中</span>
            </div>
          </div>
        </div>

        {/* 处理选项 */}
        <div className="process-options">
          <h3>🔧 输出设置</h3>

          <div className="option-group">
            <label className="input-label">
              输出文件:
              <div className="output-path-group">
                <input
                  type="text"
                  className="text-input"
                  value={outputPath}
                  onChange={(e) => setOutputPath(e.target.value)}
                  placeholder="例如: C:\\output\\merged.txt"
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
            <p className="input-hint">指定打散合并后的输出文件路径（.txt格式）</p>
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
                <span>🔀</span>
                <span>开始打散合并</span>
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

export default TxtInterleaver;
