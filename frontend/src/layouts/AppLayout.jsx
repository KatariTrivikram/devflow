import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from '../components/layout/Sidebar';
import Header from '../components/layout/Header';

export const AppLayout = () => {
  return (
    <div className="df-layout-container">
      <Sidebar />
      <div className="df-main-shell">
        <Header />
        <main className="df-main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
