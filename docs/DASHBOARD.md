# NextDungeon Dashboard

## Overview

The NextDungeon Dashboard is a web-based monitoring interface that provides a comprehensive view of all floors, running floor instances, and active web editor sessions. The dashboard is integrated into the existing BungeeCord proxy webserver.

## Access

Once the NextDungeon BungeeCord plugin is running, the dashboard is accessible at:

```
http://localhost:7734/dashboard
```

(Port 7734 is the default web editor port, configurable in `config.yml`)

## Features

### 1. Summary Statistics
- **Total Floors**: Count of all registered floors in the system
- **Active Instances**: Number of currently running floor instances
- **Active Sessions**: Number of active web editor sessions

### 2. Floors List
- Displays all floors with their ID, name, and description
- Shows active instance count per floor
- Shows active editor session count per floor
- Click on any floor row to view its detailed configuration in a modal

### 3. Instances List
- Shows all running floor instances
- Displays instance ID, associated floor, status (Ready/Preparing), and player count

### 4. Editor Sessions List
- Lists all active web editor sessions
- Shows session ID, dungeon name, floor ID, editor name, server, and creation time

### 5. Visualizations
- **Instance Distribution Chart**: Bar chart showing how many instances are running for each floor
- **Most Edited Floors Chart**: Doughnut chart showing the top 10 most frequently edited floors

### 6. Floor Configuration Modal
Click on any floor in the Floors table to view:
- General information (ID, name, description)
- World configuration
- Requirements
- Rules
- Steps count
- Triggers count

## Technical Details

### Architecture

The dashboard consists of three main components:

1. **DashboardService** (`bungeecord/src/main/java/fr/perrier/dungeons/bungee/dashboard/DashboardService.java`)
   - Accesses Redis RMaps for floors and instances
   - Aggregates data from EditorSessionManager
   - Generates JSON responses for API endpoints

2. **API Endpoints** (added to `ProxyWebEditorServer.java`)
   - `GET /dashboard` - Serves the dashboard HTML
   - `GET /dashboard/api/floors` - Returns all floors with counts
   - `GET /dashboard/api/instances` - Returns all active instances
   - `GET /dashboard/api/sessions` - Returns all active editor sessions
   - `GET /dashboard/api/stats` - Returns aggregated statistics for charts
   - `GET /dashboard/api/floor/{floorId}` - Returns detailed configuration for a specific floor

3. **UI** (`bungeecord/src/main/resources/webserver/dashboard.html`)
   - Single-page application with Chart.js for visualizations
   - Matches the visual style of the existing web editor
   - Auto-refreshes data every 30 seconds
   - Interactive tables with modal overlays

### Data Sources

- **Floors**: Redis RMap `dungeons:floors` (FloorData objects)
- **Instances**: Redis RMap `dungeons:instances` (FloorInstanceData objects)
- **Sessions**: EditorSessionManager in-memory map

### Styling

The dashboard uses the same color scheme and styling as the existing Blockly web editor:
- Gradient background: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`
- Card-based layout with shadows and hover effects
- Responsive design that adapts to different screen sizes
- Chart.js for data visualizations

## Auto-Refresh

The dashboard automatically refreshes all data every 30 seconds. You can also manually refresh by clicking the "🔄 Actualiser" button in the bottom-right corner.

## Requirements

- NextDungeon BungeeCord plugin running
- Redis server accessible
- Web browser with JavaScript enabled
- Modern browser supporting Chart.js (Chrome, Firefox, Safari, Edge)

## Troubleshooting

### Dashboard shows "Aucune donnée disponible"
- Ensure Redis is running and accessible
- Check that floors and instances are properly registered in Redis
- Verify the Redis connection in the BungeeCord plugin configuration

### API returns errors
- Check BungeeCord console logs for error messages
- Verify Redis RMap keys match: `dungeons:floors` and `dungeons:instances`
- Ensure RedissonClient is properly initialized in ProxyPidgin

### Charts not displaying
- Check browser console for JavaScript errors
- Verify Chart.js CDN is accessible
- Ensure data is being returned from `/dashboard/api/stats` endpoint
