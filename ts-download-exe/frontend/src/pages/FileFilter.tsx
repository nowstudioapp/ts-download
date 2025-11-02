import React, { useState, useRef } from 'react';
import { SelectExcelFiles, SelectSaveFile, FilterExcelFile, SelectTxtFile } from '../../wailsjs/go/main/App';
import './FileFilter.css';

export interface FilterConfig {
  excelFile: string;
  txtFile: string;
  columnName: string;
  filterType: 'include' | 'exclude';
  outputPath: string;
}

const FileFilter: React.FC = () => {
  const [excelFile, setExcelFile] = useState<string>('');
  const [txtFile, setTxtFile] = useState<string>('');
  const [columnName, setColumnName] = useState<string>('手机号码');
  const [filterType, setFilterType] = useState<'include' | 'exclude'>('include');
  const [outputPath, setOutputPath] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [isDraggingExcel, setIsDraggingExcel] = useState<boolean>(false);
  const [isDraggingTxt, setIsDraggingTxt] = useState<boolean>(false);


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

  const handleSelectTxt = async () => {
    try {
      const selectedPath = await SelectTxtFile();
      if (selectedPath && selectedPath.trim()) {
        setTxtFile(selectedPath.trim());
        addLog(`✓ 选择TXT文件: ${selectedPath.split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择TXT文件失败: ${error}`);
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

  const handleStartFilter = async () => {
    if (!excelFile || !txtFile || !outputPath || !columnName) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    addLog('🚀 开始过滤处理...');

    try {
      setProgress(20);
      addLog(`📊 正在读取Excel文件: ${excelFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`📄 正在读取TXT文件: ${txtFile.split('\\').pop()}`);
      
      setProgress(60);
      addLog(`🔍 正在执行${filterType === 'include' ? '包含' : '排除'}过滤...`);
      
      const result = await FilterExcelFile({
        excelFile,
        txtFile,
        columnName,
        filterType,
        outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog(`✅ 过滤完成！`);
        addLog(`  处理行数: ${result.rowsProcessed}`);
        addLog(`  过滤后行数: ${result.rowsFiltered}`);
        addLog(`  输出文件: ${result.outputPath}`);
        setProgress(100);
      } else {
        addLog(`❌ 过滤失败: ${result.message}`);
        setProgress(0);
      }
    } catch (error) {
      addLog(`❌ 过滤失败: ${error}`);
      setProgress(0);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClearLogs = () => {
    setLogs([]);
  };

  // Excel文件拖拽处理
  const handleExcelDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingExcel(true);
  };

  const handleExcelDragLeave = () => {
    setIsDraggingExcel(false);
  };

  const handleExcelDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingExcel(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    const excelFiles = droppedFiles.filter(file => 
      file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
    );

    if (excelFiles.length === 0) {
      addLog('⚠️ 请拖拽 Excel 文件');
      return;
    }

    const file = excelFiles[0];
    // 在 Wails 中，尝试获取完整路径
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽Excel文件路径:', fullPath);
    console.log('文件对象:', file);
    
    setExcelFile(fullPath);
    addLog(`✓ 拖拽Excel文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
  };

  // TXT文件拖拽处理
  const handleTxtDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingTxt(true);
  };

  const handleTxtDragLeave = () => {
    setIsDraggingTxt(false);
  };

  const handleTxtDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingTxt(false);

    const droppedFiles = Array.from(e.dataTransfer.files);
    const txtFiles = droppedFiles.filter(file => 
      file.name.endsWith('.txt')
    );

    if (txtFiles.length === 0) {
      addLog('⚠️ 请拖拽 TXT 文件');
      return;
    }

    const file = txtFiles[0];
    // 在 Wails 中，尝试获取完整路径
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽TXT文件路径:', fullPath);
    console.log('文件对象:', file);
    
    setTxtFile(fullPath);
    addLog(`✓ 拖拽TXT文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
  };

  return (
    <div className="file-filter">
      <div className="filter-header">
        <h2>📋 文件过滤</h2>
        <p>根据TXT文件中的数据列表，过滤Excel文件中的行</p>
      </div>

      <div className="filter-content">
        <div className="upload-section">
          {/* Excel文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box excel-upload ${isDraggingExcel ? 'dragging' : ''}`}
              onClick={handleSelectExcel}
              onDragOver={handleExcelDragOver}
              onDragLeave={handleExcelDragLeave}
              onDrop={handleExcelDrop}
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

          {/* TXT文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box txt-upload ${isDraggingTxt ? 'dragging' : ''}`}
              onClick={handleSelectTxt}
              onDragOver={handleTxtDragOver}
              onDragLeave={handleTxtDragLeave}
              onDrop={handleTxtDrop}
            >
              <div className="upload-icon">📄</div>
              <div className="upload-text">
                <div className="upload-title">选择TXT文件</div>
                <div className="upload-subtitle">点击选择或拖拽TXT文件到此处</div>
              </div>
            </div>
            {txtFile && (
              <div className="selected-file">
                <span className="file-icon">📄</span>
                <span className="file-name">{txtFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setTxtFile('')}>✕</button>
              </div>
            )}
          </div>
        </div>

        {/* 过滤选项 */}
        <div className="filter-options">
          <h3>🔧 过滤选项</h3>
          
          <div className="option-group">
            <label className="input-label">
              匹配列名:
              <input
                type="text"
                className="text-input"
                value={columnName}
                onChange={(e) => setColumnName(e.target.value)}
                placeholder="例如: 手机号码"
                disabled={isProcessing}
              />
            </label>
            <p className="input-hint">Excel文件中要匹配的列名（区分大小写）</p>
          </div>

          <div className="option-group">
            <label className="input-label">过滤条件:</label>
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
                <span className="radio-text">包含 - 保留TXT中存在的行</span>
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
                <span className="radio-text">不包含 - 删除TXT中存在的行</span>
              </label>
            </div>
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
                  placeholder="例如: C:\\output\\filtered.xlsx"
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
            <p className="input-hint">指定过滤后的输出文件路径</p>
          </div>

          <button
            className="start-button"
            onClick={handleStartFilter}
            disabled={isProcessing || !excelFile || !txtFile || !outputPath || !columnName}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                <span>处理中...</span>
              </>
            ) : (
              <>
                <span>🔍</span>
                <span>开始过滤</span>
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

export default FileFilter;
