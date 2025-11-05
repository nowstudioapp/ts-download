import React, { useState } from 'react';
import { SelectExcelFiles, SelectSaveFile, GenerateActivityData } from '../../wailsjs/go/main/App';
import './ActivityGenerator.css';

export interface ActivityConfig {
  excelFile: string;
  maxDays: number;
  outputPath: string;
}

const ActivityGenerator: React.FC = () => {
  const [excelFile, setExcelFile] = useState<string>('');
  const [maxDays, setMaxDays] = useState<number>(7);
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

  const handleStartGenerate = async () => {
    if (!excelFile || !outputPath || maxDays < 0) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    addLog('🚀 开始生成活跃数据...');

    try {
      setProgress(20);
      addLog(`📊 正在读取Excel文件: ${excelFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`🎲 正在生成活跃数据 (最大天数: ${maxDays})...`);
      
      setProgress(60);
      addLog(`📝 正在添加活跃时间和天数列...`);
      
      const result = await GenerateActivityData({
        excelFile,
        maxDays,
        outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog('✅ 活跃数据生成完成！');
        addLog(`  处理行数: ${result.rowsProcessed}`);
        addLog(`  添加列数: 2 (活跃时间、天数)`);
        addLog(`  输出文件: ${result.outputPath}`);
        setProgress(100);
      } else {
        addLog(`❌ 生成失败: ${result.message}`);
        setProgress(0);
      }
      
    } catch (error) {
      addLog(`❌ 生成失败: ${error}`);
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

  // 生成示例数据说明
  const generateExampleText = () => {
    const today = new Date();
    const examples = [];
    
    for (let i = 0; i <= Math.min(maxDays, 3); i++) {
      const minDays = i;
      const maxDaysRange = i + 1;
      const randomDay = Math.floor(Math.random() * (i + 1));
      
      const startDate = new Date(today);
      startDate.setDate(today.getDate() - maxDaysRange);
      const endDate = new Date(today);
      endDate.setDate(today.getDate() - minDays);
      
      examples.push(
        `天数=${randomDay} (0-${i}随机), 活跃时间=${startDate.toLocaleDateString()} ~ ${endDate.toLocaleDateString()}`
      );
    }
    
    return examples;
  };

  return (
    <div className="activity-generator">
      <div className="generator-header">
        <h2>📊 活跃数据生成</h2>
        <p>为Excel文件添加活跃时间和天数列，根据设定的最大天数生成随机活跃数据</p>
      </div>

      <div className="generator-content">
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

        {/* 生成选项 */}
        <div className="generate-options">
          <h3>🔧 生成选项</h3>
          
          <div className="options-grid">
            <div className="option-group">
              <label className="input-label">
                最大天数:
                <input
                  type="number"
                  className="text-input number-input"
                  value={maxDays}
                  onChange={(e) => {
                    const value = e.target.value;
                    if (value === '') {
                      setMaxDays(0);
                    } else {
                      setMaxDays(Number(value) || 0);
                    }
                  }}
                  min="0"
                  max="365"
                  placeholder="7"
                  disabled={isProcessing}
                />
              </label>
              <p className="input-hint">生成数据的最大天数范围</p>
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
              <p className="input-hint">生成后的文件保存路径</p>
            </div>
          </div>

          <div className="example-section">
            <h4>📋 生成规则</h4>
            <p>根据最大天数 <strong>{maxDays}</strong> 生成活跃数据：</p>
            <div className="rule-explanation">
              <div className="rule-item">
                <strong>天数列：</strong>随机生成 0 到 {maxDays} 之间的整数
              </div>
              <div className="rule-item">
                <strong>活跃时间列：</strong>根据天数值生成对应时间范围内的随机时间
              </div>
            </div>
            <div className="example-list">
              <h5>示例数据：</h5>
              {generateExampleText().map((example, index) => (
                <div key={index} className="example-item">{example}</div>
              ))}
            </div>
          </div>

          <button 
            className="start-button"
            onClick={handleStartGenerate}
            disabled={isProcessing || !excelFile || !outputPath}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                生成中...
              </>
            ) : (
              <>
                🚀 开始生成
              </>
            )}
          </button>
        </div>

        {/* 进度条 */}
        <div className="progress-section">
          <h3>📊 生成进度</h3>
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
            <h3>📝 生成日志</h3>
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

export default ActivityGenerator;
