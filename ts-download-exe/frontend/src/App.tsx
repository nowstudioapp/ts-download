import React, { useState } from 'react';
import TabBar from './components/TabBar';
import FileMerge from './pages/FileMerge';
import FileFilter from './pages/FileFilter';
import FileUpdate from './pages/FileUpdate';
import AgeProcessor from './pages/AgeProcessor';
import ActivityGenerator from './pages/ActivityGenerator';
import TxtProcessor from './pages/TxtProcessor';
import PhoneSplitter from './pages/PhoneSplitter';
import TxtInterleaver from './pages/TxtInterleaver';
import './App.css';

export type TabType = 'file-merge' | 'file-update' | 'file-filter' | 'file-duplicate' | 'activity-generator' | 'txt-processor' | 'phone-splitter' | 'txt-interleaver';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('file-merge');

  const renderContent = () => {
    switch (activeTab) {
      case 'file-merge':
        return <FileMerge />;
      case 'file-update':
        return <FileUpdate />;
      case 'file-filter':
        return <FileFilter />;
      case 'file-duplicate':
        return <AgeProcessor />;
      case 'activity-generator':
        return <ActivityGenerator />;
      case 'txt-processor':
        return <TxtProcessor />;
      case 'phone-splitter':
        return <PhoneSplitter />;
      case 'txt-interleaver':
        return <TxtInterleaver />;
      default:
        return <div className="placeholder">未知页面</div>;
    }
  };

  return (
    <div className="app">
      <TabBar activeTab={activeTab} onTabChange={setActiveTab} />
      <div className="content">
        {renderContent()}
      </div>
    </div>
  );
};

export default App;
