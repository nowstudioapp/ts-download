import React from 'react';
import { TabType } from '../App';
import './TabBar.css';

interface TabBarProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
}

interface Tab {
  id: TabType;
  label: string;
  icon: string;
}

const tabs: Tab[] = [
  { id: 'file-merge', label: '文件合并', icon: '📋' },
  { id: 'file-update', label: '文件更新', icon: '🔄' },
  { id: 'file-filter', label: '文件过滤', icon: '🔍' },
  { id: 'file-duplicate', label: '年龄处理', icon: '🎂' },
  { id: 'activity-generator', label: '活跃数据', icon: '📊' },
  { id: 'txt-processor', label: 'TXT处理', icon: '📝' },
  { id: 'phone-splitter', label: '号码拆分', icon: '📱' },
  { id: 'txt-interleaver', label: '文本打散', icon: '🔀' }
];

const TabBar: React.FC<TabBarProps> = ({ activeTab, onTabChange }) => {
  return (
    <div className="tab-bar">
      <div className="app-title">
        <span className="app-icon">📊</span>
        <span className="app-name">TS-Merge v1.0.0 - 高性能文件处理工具</span>
      </div>
      <div className="tabs">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            className={`tab ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => onTabChange(tab.id)}
          >
            <span className="tab-icon">{tab.icon}</span>
            <span className="tab-label">{tab.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
};

export default TabBar;
