import React from 'react';
import Drawer from '@mui/material/Drawer';
const sideBar_Lateral = ({open, drawerWidth = 'auto', anchor = 'auto', children}) => {
    return (
        <Drawer
      variant="persistent"
      anchor={anchor}
      open={open}
      sx={{
        width: open ? drawerWidth : 0,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
          padding: 0,
          position: 'relative',
          overflow: 'visible',
        },
      }}
    >
      {children}
    </Drawer>
    );
};

export default sideBar_Lateral;