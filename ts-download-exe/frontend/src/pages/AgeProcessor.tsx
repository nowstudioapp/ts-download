import React, { useState } from 'react';
import { SelectExcelFiles, SelectSaveFile, ProcessAgeColumn } from '../../wailsjs/go/main/App';
import './AgeProcessor.css';

export interface AgeProcessConfig {
  excelFile: string;
  ageColumn: string;
  threshold: number;
  increment: number;
  outputPath: string;
}

const AgeProcessor: React.FC = () => {
  const [excelFile, setExcelFile] = useState<string>('');
  const [ageColumn, setAgeColumn] = useState<string>('年龄');
  const [threshold, setThreshold] = useState<number>(18);
  const [increment, setIncrement] = useState<number>(5);
  const [outputPath, setOutputPath] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [isDragging, setIsDragging] = useState<boolean>(false);

  const addLog = (message: string) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${message}`]);
  };

  const handleSelectExcel = async () => {
    try {
      const selectedPaths = await SelectExcelFiles();
      if (selectedPaths && selectedPaths.length > 0) {
        setExcelFile(selectedPaths[0]);
        addLog(`✓ 选择Excel文件: ${selectedPaths[0].split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择Excel文件失败: ${error}`);
    }
  };

  const handleSelectOutput = async () => {
    try {
      const path = await SelectSaveFile();
      if (path && path.trim()) {
        setOutputPath(path.trim());
        addLog(`✓ 设置输出路径: ${path.split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择输出路径失败: ${error}`);
    }
  };

  const handleStartProcess = async () => {
    if (!excelFile || !outputPath || !ageColumn || threshold < 0 || increment <= 0) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    addLog('🚀 开始处理年龄数据...');

    try {
      setProgress(20);
      addLog(`📊 正在读取Excel文件: ${excelFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`🔍 正在查找年龄列: ${ageColumn}`);
      
      setProgress(60);
      addLog(`📝 正在处理年龄数据 (阈值: ${threshold}, 增量: ${increment})...`);
      
      const result = await ProcessAgeColumn({
        excelFile,
        ageColumn,
        threshold,
        increment,
        outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog('✅ 年龄处理完成！');
        addLog(`  处理行数: ${result.rowsProcessed}`);
        addLog(`  修改行数: ${result.rowsModified}`);
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

  // 拖拽处理
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
      file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
    );

    if (excelFiles.length === 0) {
      addLog('⚠️ 请拖拽 Excel 文件');
      return;
    }

    const file = excelFiles[0];
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽Excel文件路径:', fullPath);
    
    setExcelFile(fullPath);
    addLog(`✓ 拖拽Excel文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
  };

  return (
    <div className="age-processor">
      <div className="processor-header">
        <h2>🎂 年龄数据处理</h2>
        <p>对Excel文件中的年龄字段进行条件处理，低于阈值的数据自动增加指定数值</p>
      </div>

      <div className="processor-content">
        {/* 文件上传区域 */}
        <div className="upload-section">
          <div className="upload-area">
            <div 
              className={`upload-box ${isDragging ? 'dragging' : ''}`}
              onClick={handleSelectExcel}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              <div className="upload-icon">📊</div>
              <div className="upload-text">
                <div className="upload-title">选择Excel文件</div>
                <div className="upload-subtitle">点击选择或拖拽Excel文件到此处</div>
              </div>
            </div>
            {excelFile && (
              <div className="selected-file">
                <span className="file-icon">📊</span>
                <span className="file-name">{excelFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setExcelFile('')}>✕</button>
              </div>
            )}
          </div>
        </div>

        {/* 处理选项 */}
        <div className="process-options">
          <h3>🔧 处理选项</h3>
          
          <div className="options-grid">
            <div className="option-group">
              <label className="input-label">
                年龄列名:
                <input
                  type="text"
                  className="text-input"
                  value={ageColumn}
                  onChange={(e) => setAgeColumn(e.target.value)}
                  placeholder="年龄"
                  disabled={isProcessing}
                />
              </label>
              <p className="input-hint">包含年龄数据的列名</p>
            </div>

            <div className="option-group">
              <label className="input-label">
                年龄阈值:
                <input
                  type="number"
                  className="text-input number-input"
                  value={threshold || ''}
                  onChange={(e) => setThreshold(Number(e.target.value) || 0)}
                  min="0"
                  max="200"
                  placeholder="18"
                  disabled={isProcessing}
                />
              </label>
              <p className="input-hint">低于此年龄的数据将被处理</p>
            </div>

            <div className="option-group">
              <label className="input-label">
                增加数值:
                <input
                  type="number"
                  className="text-input number-input"
                  value={increment || ''}
                  onChange={(e) => setIncrement(Number(e.target.value) || 1)}
                  min="1"
                  max="100"
                  placeholder="5"
                  disabled={isProcessing}
                />
              </label>
              <p className="input-hint">符合条件的年龄将增加此数值</p>
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
                    placeholder="选择保存位置..."
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
              <p className="input-hint">处理后的文件保存路径</p>
            </div>
          </div>

          <div className="example-section">
            <h4>📋 处理示例</h4>
            <p>如果设置阈值为 <strong>{threshold}</strong>，增量为 <strong>{increment}</strong>：</p>
            <ul>
              <li>年龄 {threshold - 1} → {threshold - 1 + increment} (符合条件，增加 {increment})</li>
              <li>年龄 {threshold} → {threshold} (不符合条件，保持不变)</li>
              <li>年龄 {threshold + 5} → {threshold + 5} (不符合条件，保持不变)</li>
            </ul>
          </div>

          <button 
            className="start-button"
            onClick={handleStartProcess}
            disabled={isProcessing || !excelFile || !outputPath}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                处理中...
              </>
            ) : (
              <>
                🚀 开始处理
              </>
            )}
          </button>
        </div>

        {/* 进度条 */}
        <div className="progress-section">
          <h3>📊 处理进度</h3>
          <div className="progress-bar">
            <div 
              className="progress-fill" 
              style={{ width: `${progress}%` }}
            ></div>
            <div className="progress-text">{progress}%</div>
          </div>
        </div>

        {/* 日志区域 */}
        <div className="log-section">
          <div className="log-header">
            <h3>📝 处理日志</h3>
            <button className="clear-button" onClick={handleClearLogs}>
              清空日志
            </button>
          </div>
          <div className="log-content">
            {logs.length === 0 ? (
              <div className="log-empty">暂无日志信息</div>
            ) : (
              logs.map((log, index) => (
                <div key={index} className="log-item">{log}</div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AgeProcessor;
