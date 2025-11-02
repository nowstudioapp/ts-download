import React, { useState, useRef } from 'react';
import { SelectExcelFiles, SelectSaveFile, UpdateExcelFile } from '../../wailsjs/go/main/App';
import './FileUpdate.css';

export interface UpdateConfig {
  mainFile: string;
  subFile: string;
  matchColumn: string;
  updateColumns: string[];
  outputPath: string;
}

// 预设的常用列名
const COMMON_UPDATE_COLUMNS = [
  'UID',
  'userName', 
  '是否会员',
  '最后上线时间',
  '有效天数',
  '姓名',
  '地址',
  '邮箱',
  '状态',
  '备注'
];

const FileUpdate: React.FC = () => {
  const [mainFile, setMainFile] = useState<string>('');
  const [subFile, setSubFile] = useState<string>('');
  const [matchColumn, setMatchColumn] = useState<string>('手机号码');
  const [updateColumns, setUpdateColumns] = useState<string>('UID,userName,是否会员,最后上线时间,有效天数');
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
      const selectedPaths = await SelectExcelFiles();
      if (selectedPaths && selectedPaths.length > 0) {
        setMainFile(selectedPaths[0]);
        addLog(`✓ 选择主文件: ${selectedPaths[0].split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择主文件失败: ${error}`);
    }
  };

  const handleSelectSubFile = async () => {
    try {
      const selectedPaths = await SelectExcelFiles();
      if (selectedPaths && selectedPaths.length > 0) {
        setSubFile(selectedPaths[0]);
        addLog(`✓ 选择副文件: ${selectedPaths[0].split('\\').pop()}`);
      }
    } catch (error) {
      addLog(`❌ 选择副文件失败: ${error}`);
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

  const handleStartUpdate = async () => {
    if (!mainFile || !subFile || !outputPath || !matchColumn || !updateColumns) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    addLog('🚀 开始文件更新处理...');

    try {
      setProgress(20);
      addLog(`📊 正在读取主文件: ${mainFile.split('\\').pop()}`);
      
      setProgress(40);
      addLog(`📄 正在读取副文件: ${subFile.split('\\').pop()}`);
      
      setProgress(60);
      addLog(`🔄 正在根据${matchColumn}匹配并更新数据...`);
      
      const result = await UpdateExcelFile({
        mainFile,
        subFile,
        matchColumn,
        updateColumns: updateColumns.split(',').map(col => col.trim()),
        outputPath
      });

      setProgress(90);

      if (result.success) {
        addLog('✅ 文件更新完成！');
        addLog(`  处理行数: ${result.rowsProcessed}`);
        addLog(`  更新行数: ${result.rowsUpdated}`);
        addLog(`  匹配字段: ${matchColumn}`);
        addLog(`  更新字段: ${updateColumns}`);
        addLog(`  输出文件: ${result.outputPath}`);
        setProgress(100);
      } else {
        addLog(`❌ 更新失败: ${result.message}`);
        setProgress(0);
      }
      
    } catch (error) {
      addLog(`❌ 更新失败: ${error}`);
      setProgress(0);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClearLogs = () => {
    setLogs([]);
  };

  // 快速添加常用列名
  const handleAddCommonColumn = (columnName: string) => {
    const currentColumns = updateColumns.split(',').map(col => col.trim()).filter(col => col);
    if (!currentColumns.includes(columnName)) {
      const newColumns = [...currentColumns, columnName].join(',');
      setUpdateColumns(newColumns);
      addLog(`✓ 添加列名: ${columnName}`);
    }
  };

  // 使用预设模板
  const handleUseTemplate = () => {
    const templateColumns = 'UID,userName,是否会员,最后上线时间,有效天数';
    setUpdateColumns(templateColumns);
    addLog('✓ 使用预设模板');
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
    console.log('拖拽主文件路径:', fullPath);
    console.log('文件对象:', file);
    
    setMainFile(fullPath);
    addLog(`✓ 拖拽主文件: ${file.name}`);
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
    console.log('拖拽副文件路径:', fullPath);
    console.log('文件对象:', file);
    
    setSubFile(fullPath);
    addLog(`✓ 拖拽副文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
  };

  return (
    <div className="file-update">
      <div className="update-header">
        <h2>🔄 文件更新</h2>
        <p>根据手机号码匹配，用副文件的数据更新主文件的指定字段</p>
      </div>

      <div className="update-content">
        <div className="upload-section">
          {/* 主文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box main-upload ${isDraggingMain ? 'dragging' : ''}`}
              onClick={handleSelectMainFile}
              onDragOver={handleMainDragOver}
              onDragLeave={handleMainDragLeave}
              onDrop={handleMainDrop}
            >
              <div className="upload-icon">📊</div>
              <div className="upload-text">
                <div className="upload-title">选择主文件</div>
                <div className="upload-subtitle">点击选择或拖拽Excel文件到此处</div>
              </div>
            </div>
            {mainFile && (
              <div className="selected-file">
                <span className="file-icon">📊</span>
                <span className="file-name">{mainFile.split('\\').pop()}</span>
                <button className="remove-btn" onClick={() => setMainFile('')}>✕</button>
              </div>
            )}
          </div>

          {/* 副文件上传区域 */}
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
                <div className="upload-title">选择副文件</div>
                <div className="upload-subtitle">点击选择或拖拽Excel文件到此处</div>
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

        {/* 更新选项 */}
        <div className="update-options">
          <h3>🔧 更新选项</h3>
          
          <div className="option-group">
            <label className="input-label">
              匹配字段:
              <input
                type="text"
                className="text-input"
                value={matchColumn}
                onChange={(e) => setMatchColumn(e.target.value)}
                placeholder="例如: 手机号码"
                disabled={isProcessing}
              />
            </label>
            <p className="input-hint">用于匹配两个文件的字段名（通常是手机号码）</p>
          </div>

          <div className="option-group">
            <label className="input-label">
              要更新的字段:
              <input
                type="text"
                className="text-input"
                value={updateColumns}
                onChange={(e) => setUpdateColumns(e.target.value)}
                placeholder="例如: 姓名,地址,邮箱"
                disabled={isProcessing}
              />
            </label>
            <p className="input-hint">要从副文件更新到主文件的字段名，多个字段用逗号分隔</p>
            
            {/* 快速选择按钮 */}
            <div className="quick-select-section">
              <div className="quick-select-header">
                <span className="quick-select-title">快速选择:</span>
                <button 
                  className="template-button"
                  onClick={handleUseTemplate}
                  disabled={isProcessing}
                >
                  使用预设模板
                </button>
              </div>
              <div className="quick-select-buttons">
                {COMMON_UPDATE_COLUMNS.map((columnName) => (
                  <button
                    key={columnName}
                    className="column-button"
                    onClick={() => handleAddCommonColumn(columnName)}
                    disabled={isProcessing}
                  >
                    + {columnName}
                  </button>
                ))}
              </div>
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
                  placeholder="例如: C:\\output\\updated.xlsx"
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
            <p className="input-hint">指定更新后的输出文件路径</p>
          </div>

          <button
            className="start-button"
            onClick={handleStartUpdate}
            disabled={isProcessing || !mainFile || !subFile || !outputPath || !matchColumn || !updateColumns}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                <span>处理中...</span>
              </>
            ) : (
              <>
                <span>🔄</span>
                <span>开始更新</span>
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

export default FileUpdate;
