import React, { useState } from 'react';
import { SelectTxtFile, SelectFolder, SplitPhoneNumbers } from '../../wailsjs/go/main/App';
import './PhoneSplitter.css';

export interface PhoneSplitConfig {
  inputFile: string;
  outputDir: string;
}

export interface PhoneSplitResult {
  success: boolean;
  message: string;
  outputDir: string;
  totalNumbers: number;
  splitResults: { [key: string]: number };
}

const PhoneSplitter: React.FC = () => {
  const [inputFile, setInputFile] = useState<string>('');
  const [outputDir, setOutputDir] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [splitResults, setSplitResults] = useState<{ [key: string]: number }>({});

  const addLog = (message: string) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${message}`]);
  };

  const handleSelectInputFile = async () => {
    try {
      const selectedPath = await SelectTxtFile();
      if (selectedPath && selectedPath.trim()) {
        setInputFile(selectedPath.trim());
        addLog(`✓ 选择输入文件: ${selectedPath.split('\\').pop()}`);
        
        // 自动设置输出目录为输入文件所在目录的split_output子文件夹
        const dir = selectedPath.substring(0, selectedPath.lastIndexOf('\\'));
        const outputPath = dir + '\\split_output';
        setOutputDir(outputPath);
        addLog(`✓ 自动设置输出目录: ${outputPath}`);
      }
    } catch (error) {
      addLog(`❌ 选择输入文件失败: ${error}`);
    }
  };

  const handleSelectOutputDir = async () => {
    try {
      // 使用文件夹选择对话框
      const selectedPath = await SelectFolder();
      if (selectedPath && selectedPath.trim()) {
        setOutputDir(selectedPath.trim());
        addLog(`✓ 设置输出目录: ${selectedPath.split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择输出目录失败: ${error}`);
    }
  };

  const handleStartSplit = async () => {
    if (!inputFile || !outputDir) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    setSplitResults({});
    addLog('🚀 开始手机号拆分处理...');

    try {
      setProgress(20);
      addLog(`📄 正在读取文件: ${inputFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`🔍 正在分析手机号长度...`);
      
      setProgress(60);
      addLog(`📂 正在创建输出目录: ${outputDir}`);
      
      const result = await SplitPhoneNumbers({
        inputFile,
        outputDir
      });

      setProgress(90);

      if (result.success) {
        addLog(`✅ 拆分完成！`);
        addLog(`  总手机号数量: ${result.totalNumbers}`);
        
        // 显示拆分结果
        Object.entries(result.splitResults).forEach(([length, count]) => {
          addLog(`  ${length}位号码: ${count} 个`);
        });
        
        addLog(`  输出目录: ${result.outputDir}`);
        setSplitResults(result.splitResults);
        setProgress(100);
      } else {
        addLog(`❌ 拆分失败: ${result.message}`);
        setProgress(0);
      }
    } catch (error) {
      addLog(`❌ 拆分失败: ${error}`);
      setProgress(0);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClearLogs = () => {
    setLogs([]);
    setSplitResults({});
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
    const txtFiles = droppedFiles.filter(file => 
      file.name.endsWith('.txt')
    );

    if (txtFiles.length === 0) {
      addLog('⚠️ 请拖拽 TXT 文件');
      return;
    }

    const file = txtFiles[0];
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽文件路径:', fullPath);
    
    setInputFile(fullPath);
    addLog(`✓ 拖拽文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
    
    // 自动设置输出目录
    const dir = fullPath.substring(0, fullPath.lastIndexOf('\\'));
    const outputPath = dir + '\\split_output';
    setOutputDir(outputPath);
    addLog(`✓ 自动设置输出目录: ${outputPath}`);
  };

  return (
    <div className="phone-splitter">
      <div className="splitter-header">
        <h2>📱 手机号拆分</h2>
        <p>根据手机号长度将TXT文件拆分成不同的文件</p>
      </div>

      <div className="splitter-content">
        <div className="phone-upload-section">
          {/* 输入文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box input-upload ${isDragging ? 'dragging' : ''}`}
              onClick={handleSelectInputFile}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              <div className="upload-icon">📄</div>
              <div className="upload-text">
                <div className="upload-title">选择手机号TXT文件</div>
                <div className="upload-subtitle">点击选择或拖拽TXT文件到此处</div>
              </div>
            </div>
            {inputFile && (
              <div className="selected-file">
                <span className="file-icon">📄</span>
                <span className="file-name">{inputFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setInputFile('')}>✕</button>
              </div>
            )}
          </div>
        </div>

        {/* 处理选项 */}
        <div className="process-options">
          <h3>🔧 拆分选项</h3>
          
          <div className="option-group">
            <label className="input-label">
              输出目录:
              <div className="output-path-group">
                <input
                  type="text"
                  className="text-input"
                  value={outputDir}
                  onChange={(e) => setOutputDir(e.target.value)}
                  placeholder="例如: C:\\output\\split_output"
                  disabled={isProcessing}
                />
                <button
                  className="browse-button"
                  onClick={handleSelectOutputDir}
                  disabled={isProcessing}
                >
                  浏览
                </button>
              </div>
            </label>
            <p className="input-hint">指定拆分后文件的输出目录</p>
          </div>

          <button
            className="start-button"
            onClick={handleStartSplit}
            disabled={isProcessing || !inputFile || !outputDir}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                <span>拆分中...</span>
              </>
            ) : (
              <>
                <span>📱</span>
                <span>开始拆分</span>
              </>
            )}
          </button>
        </div>

        {/* 进度条 */}
        {isProcessing && (
          <div className="progress-section">
            <h3>📊 拆分进度</h3>
            <div className="progress-bar">
              <div className="progress-fill" style={{ width: `${progress}%` }}></div>
              <span className="progress-text">{progress}%</span>
            </div>
          </div>
        )}

        {/* 拆分结果统计 */}
        {Object.keys(splitResults).length > 0 && (
          <div className="results-section">
            <h3>📈 拆分结果</h3>
            <div className="results-grid">
              {Object.entries(splitResults).map(([length, count]) => (
                <div key={length} className="result-item">
                  <div className="result-length">{length}位</div>
                  <div className="result-count">{count} 个</div>
                </div>
              ))}
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

export default PhoneSplitter;
