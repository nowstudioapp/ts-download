import React from 'react';
import './ProgressBar.css';

interface ProgressBarProps {
  progress: number;
}

const ProgressBar: React.FC<ProgressBarProps> = ({ progress }) => {
  return (
    <div className="progress-bar-container">
      <div className="progress-header">
        <span className="progress-icon">📊</span>
        <span className="progress-title">进度状态</span>
      </div>
      <div className="progress-bar">
        <div className="progress-fill" style={{ width: `${progress}%` }}>
          <span className="progress-text">{progress}%</span>
        </div>
      </div>
    </div>
  );
};

export default ProgressBar;
