import React, { useState } from 'react';
import { SelectExcelFiles, SelectSaveFile, RemoveChineseRows } from '../../wailsjs/go/main/App';
import './ChineseRemover.css';

export interface ChineseRemoveConfig {
  excelFile: string;
  outputPath: string;
  checkedColumns: string[];
  charType: string;
  action: string;
}

const ChineseRemover: React.FC = () => {
  const [excelFile, setExcelFile] = useState<string>('');
  const [outputPath, setOutputPath] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [columnInput, setColumnInput] = useState<string>('');
  const [charType, setCharType] = useState<string>('chinese');
  const [action, setAction] = useState<string>('remove');

  const addLog = (message: string) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${message}`]);
  };

  const handleSelectExcel = async () => {
    try {
      const selectedPaths = await SelectExcelFiles();
      if (selectedPaths && selectedPaths.length > 0) {
        const filePath = selectedPaths[0];
        setExcelFile(filePath);
        addLog(`✓ 选择Excel文件: ${filePath.split('\\').pop()}`);
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
    if (!excelFile || !outputPath) {
      addLog('❌ 请完善所有必填项');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    const charTypeLabel: Record<string, string> = { chinese: '中文', english: '英文', both: '中文或英文' };
    const actionLabel: Record<string, string> = { remove: '删除', keep: '保留' };
    addLog(`🚀 开始处理... 模式: ${actionLabel[action]}包含${charTypeLabel[charType]}的行`);

    try {
      setProgress(20);
      addLog(`📊 正在读取Excel文件: ${excelFile.split('\\').pop()}`);
      
      const checkedColumns = columnInput.trim() 
        ? columnInput.split(',').map(col => col.trim()).filter(col => col !== '')
        : [];
      
      setProgress(50);
      if (checkedColumns.length > 0) {
        addLog(`🔍 检查指定列: ${checkedColumns.join(', ')}`);
      } else {
        addLog('🔍 检查所有列（除表头外）');
      }
      addLog(`🔍 正在扫描并${actionLabel[action]}包含${charTypeLabel[charType]}的行...`);
      
      const result = await RemoveChineseRows({
        excelFile,
        outputPath,
        checkedColumns,
        charType,
        action
      });

      setProgress(90);

      if (result.success) {
        addLog(`✅ 处理完成！`);
        addLog(`  总行数: ${result.rowsProcessed}`);
        addLog(`  删除行数: ${result.rowsRemoved}`);
        addLog(`  保留行数: ${result.rowsKept}`);
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

  // Excel文件拖拽处理
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
    
    setExcelFile(fullPath);
    addLog(`✓ 拖拽Excel文件: ${file.name}`);
  };

  return (
    <div className="chinese-remover">
      <div className="remover-header">
        <h2>🈳 中英文处理</h2>
        <p>根据指定列是否包含中文/英文字符，保留或删除整行数据（标点符号不算，保留表头）</p>
      </div>

      <div className="remover-content">
        <div className="upload-section">
          {/* Excel文件上传区域 */}
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
          
          <div className="option-group">
            <div className="info-box">
              <p>📌 处理说明：</p>
              <ul>
                <li>✓ 选择要检测的字符类型：中文、英文、或两者都检测</li>
                <li>✓ 选择操作方式：保留或删除包含指定字符的行</li>
                <li>✓ 输入要检查的列名（多个列名用逗号分隔）</li>
                <li>✓ 不输入列名则检查所有列（除表头外）</li>
                <li>✓ 标点符号不计入检测范围</li>
              </ul>
            </div>
          </div>

          {/* 字符类型选择 */}
          <div className="option-group">
            <label className="input-label">检测字符类型:</label>
            <div className="radio-group">
              <label className="radio-label">
                <input type="radio" name="charType" value="chinese" checked={charType === 'chinese'} onChange={(e) => setCharType(e.target.value)} disabled={isProcessing} />
                <span>中文</span>
              </label>
              <label className="radio-label">
                <input type="radio" name="charType" value="english" checked={charType === 'english'} onChange={(e) => setCharType(e.target.value)} disabled={isProcessing} />
                <span>英文</span>
              </label>
              <label className="radio-label">
                <input type="radio" name="charType" value="both" checked={charType === 'both'} onChange={(e) => setCharType(e.target.value)} disabled={isProcessing} />
                <span>中文或英文</span>
              </label>
            </div>
          </div>

          {/* 操作方式选择 */}
          <div className="option-group">
            <label className="input-label">操作方式:</label>
            <div className="radio-group">
              <label className="radio-label">
                <input type="radio" name="action" value="remove" checked={action === 'remove'} onChange={(e) => setAction(e.target.value)} disabled={isProcessing} />
                <span>删除匹配行</span>
              </label>
              <label className="radio-label">
                <input type="radio" name="action" value="keep" checked={action === 'keep'} onChange={(e) => setAction(e.target.value)} disabled={isProcessing} />
                <span>保留匹配行</span>
              </label>
            </div>
            <p className="input-hint">
              {action === 'remove'
                ? `将删除指定列中包含${charType === 'chinese' ? '中文' : charType === 'english' ? '英文' : '中文或英文'}的整行数据`
                : `将只保留指定列中包含${charType === 'chinese' ? '中文' : charType === 'english' ? '英文' : '中文或英文'}的整行数据`}
            </p>
          </div>

          {/* 列名输入 */}
          <div className="option-group">
            <label className="input-label">
              要检查的列名（可选）:
              <input
                type="text"
                className="text-input"
                value={columnInput}
                onChange={(e) => setColumnInput(e.target.value)}
                placeholder="例如：姓名,地址,备注（多个用逗号分隔，留空则检查所有列）"
                disabled={isProcessing}
              />
            </label>
            <p className="input-hint">输入要检查的列名，多个列名用英文逗号分隔；留空则检查所有列</p>
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
                  placeholder="例如: C:\\output\\no_chinese.xlsx"
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
            <p className="input-hint">指定处理后的输出文件路径</p>
          </div>

          <button
            className="start-button"
            onClick={handleStartProcess}
            disabled={isProcessing || !excelFile || !outputPath}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                <span>处理中...</span>
              </>
            ) : (
              <>
                <span>🈳</span>
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

export default ChineseRemover;

