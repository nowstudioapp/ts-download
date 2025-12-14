import React, { useState, useEffect } from 'react';
import { SelectExcelFile, SelectFolder, GetCountryList, GetExcelHeaders, SplitByAreaCode } from '../../wailsjs/go/main/App';
import { main } from '../../wailsjs/go/models';
import './AreaCodeSplitter.css';

type CountryOption = main.CountryOption;

const AreaCodeSplitter: React.FC = () => {
  const [excelFile, setExcelFile] = useState<string>('');
  const [phoneColumn, setPhoneColumn] = useState<string>('');
  const [countryCode, setCountryCode] = useState<string>('');
  const [outputDir, setOutputDir] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState<number>(0);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const [splitResults, setSplitResults] = useState<{ [key: string]: number }>({});
  const [countries, setCountries] = useState<CountryOption[]>([]);
  const [headers, setHeaders] = useState<string[]>([]);
  const [countrySearch, setCountrySearch] = useState<string>('');

  useEffect(() => {
    loadCountries();
  }, []);

  const loadCountries = async () => {
    try {
      const countryList = await GetCountryList();
      setCountries(countryList || []);
      addLog('✓ 国家列表加载完成');
    } catch (error: unknown) {
      addLog(`❌ 加载国家列表失败: ${String(error)}`);
    }
  };

  const addLog = (message: string) => {
    setLogs(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${message}`]);
  };

  const handleSelectExcelFile = async () => {
    try {
      const selectedPath = await SelectExcelFile();
      if (selectedPath && selectedPath.trim()) {
        setExcelFile(selectedPath.trim());
        addLog(`✓ 选择Excel文件: ${selectedPath.split('\\').pop()}`);
        
        // 自动获取表头
        try {
          const fileHeaders = await GetExcelHeaders(selectedPath);
          setHeaders(fileHeaders || []);
          addLog(`✓ 获取表头成功，共 ${fileHeaders?.length || 0} 列`);
        } catch (err: unknown) {
          addLog(`❌ 获取表头失败: ${String(err)}`);
        }
        
        // 自动设置输出目录
        const dir = selectedPath.substring(0, selectedPath.lastIndexOf('\\'));
        const outputPath = dir + '\\area_split_output';
        setOutputDir(outputPath);
        addLog(`✓ 自动设置输出目录: ${outputPath}`);
      }
    } catch (error: unknown) {
      addLog(`❌ 选择Excel文件失败: ${String(error)}`);
    }
  };

  const handleSelectOutputDir = async () => {
    try {
      const selectedPath = await SelectFolder();
      if (selectedPath && selectedPath.trim()) {
        setOutputDir(selectedPath.trim());
        addLog(`✓ 设置输出目录: ${selectedPath}`);
      }
    } catch (error: unknown) {
      addLog(`❌ 选择输出目录失败: ${String(error)}`);
    }
  };

  const handleStartSplit = async () => {
    if (!excelFile) {
      addLog('❌ 请选择Excel文件');
      return;
    }
    if (!phoneColumn) {
      addLog('❌ 请选择手机号字段列');
      return;
    }
    if (!countryCode) {
      addLog('❌ 请选择国家');
      return;
    }
    if (!outputDir) {
      addLog('❌ 请选择输出目录');
      return;
    }

    setIsProcessing(true);
    setProgress(0);
    setSplitResults({});
    addLog('🚀 开始区号拆分处理...');

    try {
      setProgress(20);
      addLog(`📄 正在读取文件: ${excelFile.split('\\').pop()}`);
      
      setProgress(40);
      const selectedCountry = countries.find(c => c.code === countryCode);
      addLog(`🌍 选择国家: ${selectedCountry?.name || countryCode}`);
      addLog(`📞 手机号字段: ${phoneColumn}`);
      
      setProgress(60);
      addLog(`📂 正在处理数据...`);
      
      const result = await SplitByAreaCode({
        excelFile,
        phoneColumn,
        countryCode,
        outputDir
      });

      setProgress(90);

      if (result.success) {
        addLog(`✅ 拆分完成！`);
        addLog(`  总数据行数: ${result.totalRows}`);
        
        // 显示拆分结果
        const sortedResults = Object.entries(result.splitResults).sort((a, b) => b[1] - a[1]);
        sortedResults.forEach(([region, count]) => {
          addLog(`  ${region}: ${count} 条`);
        });
        
        addLog(`  输出目录: ${result.outputDir}`);
        setSplitResults(result.splitResults);
        setProgress(100);
      } else {
        addLog(`❌ 拆分失败: ${result.message}`);
        setProgress(0);
      }
    } catch (error: unknown) {
      addLog(`❌ 拆分失败: ${String(error)}`);
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
    const excelFiles = droppedFiles.filter(file => 
      file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
    );

    if (excelFiles.length === 0) {
      addLog('⚠️ 请拖拽 Excel 文件 (.xlsx 或 .xls)');
      return;
    }

    const file = excelFiles[0];
    const fullPath = (file as any).path || (file as any).webkitRelativePath || file.name;
    console.log('拖拽文件路径:', fullPath);
    
    setExcelFile(fullPath);
    addLog(`✓ 拖拽文件: ${file.name}`);
    addLog(`  路径: ${fullPath}`);
    
    // 自动获取表头
    try {
      const fileHeaders = await GetExcelHeaders(fullPath);
      setHeaders(fileHeaders || []);
      addLog(`✓ 获取表头成功，共 ${fileHeaders?.length || 0} 列`);
    } catch (err: unknown) {
      addLog(`❌ 获取表头失败: ${String(err)}`);
    }
    
    // 自动设置输出目录
    const dir = fullPath.substring(0, fullPath.lastIndexOf('\\'));
    const outputPath = dir + '\\area_split_output';
    setOutputDir(outputPath);
    addLog(`✓ 自动设置输出目录: ${outputPath}`);
  };

  // 过滤国家列表
  const filteredCountries = countries.filter(country => 
    country.name.toLowerCase().includes(countrySearch.toLowerCase()) ||
    country.code.toLowerCase().includes(countrySearch.toLowerCase())
  );

  return (
    <div className="area-code-splitter">
      <div className="splitter-header">
        <h2>🌍 区号拆分</h2>
        <p>根据手机号区号将Excel文件按城市/地区拆分成多个文件</p>
      </div>

      <div className="splitter-content">
        <div className="upload-section">
          {/* Excel文件上传区域 */}
          <div className="upload-area">
            <div 
              className={`upload-box ${isDragging ? 'dragging' : ''}`}
              onClick={handleSelectExcelFile}
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
                <button className="remove-btn" onClick={() => {
                  setExcelFile('');
                  setHeaders([]);
                }}>✕</button>
              </div>
            )}
          </div>
        </div>

        {/* 处理选项 */}
        <div className="process-options">
          <h3>🔧 拆分选项</h3>
          
          {/* 国家选择 */}
          <div className="option-group">
            <label className="input-label">
              选择国家:
              <input
                type="text"
                className="search-input"
                placeholder="搜索国家..."
                value={countrySearch}
                onChange={(e) => setCountrySearch(e.target.value)}
                disabled={isProcessing}
              />
              <select
                className="select-input"
                value={countryCode}
                onChange={(e) => {
                  setCountryCode(e.target.value);
                  const selected = countries.find(c => c.code === e.target.value);
                  if (selected) {
                    addLog(`✓ 选择国家: ${selected.name}`);
                  }
                }}
                disabled={isProcessing}
              >
                <option value="">-- 请选择国家 --</option>
                {filteredCountries.map(country => (
                  <option key={country.code} value={country.code}>
                    {country.name} ({country.code})
                  </option>
                ))}
              </select>
            </label>
            <p className="input-hint">选择要处理的国家，系统将根据该国家的区号配置进行拆分</p>
          </div>

          {/* 手机号字段列选择 */}
          <div className="option-group">
            <label className="input-label">
              手机号字段列:
              <select
                className="select-input"
                value={phoneColumn}
                onChange={(e) => {
                  setPhoneColumn(e.target.value);
                  if (e.target.value) {
                    addLog(`✓ 选择手机号字段: ${e.target.value}`);
                  }
                }}
                disabled={isProcessing || headers.length === 0}
              >
                <option value="">-- 请选择字段 --</option>
                {headers.map((header, index) => (
                  <option key={index} value={header}>
                    {header}
                  </option>
                ))}
              </select>
            </label>
            <p className="input-hint">选择包含手机号的列（需先选择Excel文件）</p>
          </div>
          
          {/* 输出目录 */}
          <div className="option-group">
            <label className="input-label">
              输出目录:
              <div className="output-path-group">
                <input
                  type="text"
                  className="text-input"
                  value={outputDir}
                  onChange={(e) => setOutputDir(e.target.value)}
                  placeholder="例如: C:\\output\\area_split_output"
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
            <p className="input-hint">拆分后的文件将保存到此目录</p>
          </div>

          <button
            className="start-button"
            onClick={handleStartSplit}
            disabled={isProcessing || !excelFile || !phoneColumn || !countryCode || !outputDir}
          >
            {isProcessing ? (
              <>
                <span className="spinner">⏳</span>
                <span>拆分中...</span>
              </>
            ) : (
              <>
                <span>🌍</span>
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
              {Object.entries(splitResults)
                .sort((a, b) => b[1] - a[1])
                .map(([region, count]) => (
                  <div key={region} className="result-item">
                    <div className="result-region">{region}</div>
                    <div className="result-count">{count} 条</div>
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

export default AreaCodeSplitter;
