# Dashboard Manual Testing Guide

## Prerequisites

Before testing the dashboard, ensure:

1. **BungeeCord Plugin Running**: The NextDungeon BungeeCord plugin is installed and running
2. **Redis Available**: Redis server is running and accessible
3. **Test Data**: Some floors and instances should be registered in Redis for meaningful testing

## Test Scenarios

### 1. Dashboard Access

**Test**: Access the dashboard
- Navigate to `http://localhost:7734/dashboard`
- **Expected**: Dashboard loads successfully with header "📊 NextDungeon Dashboard"
- **Verify**: No console errors in browser developer tools

### 2. Summary Statistics

**Test**: Check summary cards
- Look at the three summary stat cards at the top
- **Expected**: Shows actual counts for:
  - Total Floors
  - Active Instances  
  - Active Sessions
- **Verify**: Numbers update when data changes

### 3. Floors List

**Test**: View floors table
- Scroll to "🏰 Liste des Floors" section
- **Expected**: 
  - Table displays with columns: ID, Nom, Description, Instances Actives, Sessions d'Édition
  - All registered floors appear in the table
  - Active instance/session counts show colored badges
- **Verify**: Data matches what's in Redis

### 4. Floor Configuration Modal

**Test**: Click on a floor row
- Click any row in the Floors table
- **Expected**:
  - Modal overlay appears
  - Title shows floor name
  - Configuration sections display:
    - Informations Générales
    - Configuration du Monde (if available)
    - Prérequis (if available)
    - Règles (if available)
    - Étapes count (if available)
    - Triggers count (if available)
- **Verify**: JSON data is properly formatted
- **Test**: Close modal by clicking X or outside modal area

### 5. Instances List

**Test**: View active instances
- Scroll to "🎮 Instances Actives" section
- **Expected**:
  - Table displays with columns: Instance ID, Floor ID, Statut, Joueurs
  - Status badges show "Ready" (green) or "Preparing" (orange)
  - Player count displayed correctly
- **Verify**: Matches actual running instances

### 6. Sessions List

**Test**: View editor sessions
- Scroll to "✏️ Sessions d'Édition Actives" section
- **Expected**:
  - Table displays with columns: Session ID, Dungeon, Floor, Éditeur, Serveur Spigot, Créé le
  - Timestamps are formatted correctly
  - Shows all active editor sessions
- **Verify**: Matches active sessions in EditorSessionManager

### 7. Instance Distribution Chart

**Test**: View bar chart
- Look at the first chart "📈 Distribution des Instances par Floor"
- **Expected**:
  - Bar chart displays with floor names on X-axis
  - Bars show count of instances per floor
  - Chart is interactive (hover shows values)
- **Verify**: Chart updates when instances change

### 8. Most Edited Floors Chart

**Test**: View doughnut chart
- Look at the second chart "🔧 Floors les Plus Édités"
- **Expected**:
  - Doughnut chart shows up to 10 floors
  - Legend on the right shows floor names
  - Different colors for each floor
  - Interactive (hover shows percentages)
- **Verify**: Reflects actual editing activity

### 9. Auto-Refresh

**Test**: Wait for auto-refresh
- Keep dashboard open for 30 seconds
- Create a new instance or floor in the background
- **Expected**:
  - Dashboard automatically updates after 30 seconds
  - New data appears without manual refresh
- **Verify**: Check browser console for API calls every 30 seconds

### 10. Manual Refresh

**Test**: Click refresh button
- Click the "🔄 Actualiser" button in bottom-right
- **Expected**:
  - All data refreshes immediately
  - No errors in console
  - Charts update smoothly
- **Verify**: Data is current

### 11. Empty State

**Test**: View with no data
- Clear all floors/instances from Redis (in test environment only)
- Refresh dashboard
- **Expected**:
  - Summary shows 0 for all counts
  - Tables show "Aucun(e) ... trouvé(e)"
  - Charts show "Aucune donnée disponible"
- **Verify**: No JavaScript errors

### 12. API Endpoints

**Test**: Direct API access
Using browser or curl, test each endpoint:

```bash
# Floors endpoint
curl http://localhost:7734/dashboard/api/floors

# Instances endpoint
curl http://localhost:7734/dashboard/api/instances

# Sessions endpoint
curl http://localhost:7734/dashboard/api/sessions

# Stats endpoint
curl http://localhost:7734/dashboard/api/stats

# Floor config endpoint (replace floor_1 with actual ID)
curl http://localhost:7734/dashboard/api/floor/floor_1
```

- **Expected**: Each returns valid JSON with `"success": true`
- **Verify**: Data structure matches documentation

### 13. Input Validation

**Test**: Invalid floor ID
```bash
# Try path traversal
curl http://localhost:7734/dashboard/api/floor/../../../etc/passwd

# Try directory traversal
curl http://localhost:7734/dashboard/api/floor/..%2F..%2Fetc%2Fpasswd

# Try empty ID
curl http://localhost:7734/dashboard/api/floor/
```

- **Expected**: Returns `"success": false, "error": "Invalid floor ID"`
- **Verify**: No actual file system access occurs

### 14. Responsive Design

**Test**: Different screen sizes
- Resize browser window to various widths:
  - Desktop (1400px+)
  - Tablet (768px)
  - Mobile (375px)
- **Expected**:
  - Layout adapts smoothly
  - Cards stack appropriately
  - Tables remain readable with scrolling
  - Charts resize without breaking
- **Verify**: No horizontal scrolling issues

### 15. Browser Compatibility

**Test**: Multiple browsers
- Test in Chrome, Firefox, Safari, Edge
- **Expected**:
  - Dashboard displays correctly in all browsers
  - Charts render properly
  - No browser-specific errors
- **Verify**: Chart.js compatibility

## Performance Tests

### Load Test

**Test**: Large dataset
- Add 100+ floors to Redis
- Add 50+ active instances
- Load dashboard
- **Expected**:
  - Page loads within 2 seconds
  - Charts render smoothly
  - No performance warnings
- **Verify**: Check browser performance tab

### Memory Test

**Test**: Long-running session
- Keep dashboard open for 1 hour
- Let it auto-refresh multiple times
- **Expected**:
  - No memory leaks
  - Performance remains stable
- **Verify**: Check browser task manager

## Error Handling

### Network Error

**Test**: Simulate network failure
- Disconnect from server
- Try to refresh dashboard
- **Expected**:
  - Error message displays
  - Dashboard doesn't crash
  - Retry works when connection restored

### Redis Unavailable

**Test**: Stop Redis server
- Refresh dashboard
- **Expected**:
  - API returns appropriate errors
  - Frontend shows error messages
  - Dashboard remains functional

## Security Tests

### XSS Prevention

**Test**: Malicious floor names
- Create a floor with name: `<script>alert('XSS')</script>`
- View in dashboard
- **Expected**:
  - Script does not execute
  - Name is escaped/sanitized
  - No security warnings

### CORS

**Test**: Cross-origin requests
- Try to access API from different origin
- **Expected**:
  - CORS headers allow access (Access-Control-Allow-Origin: *)
  - Requests succeed

## Logging

**Test**: Check server logs
- Perform various dashboard actions
- Check BungeeCord console
- **Expected**:
  - Appropriate log messages for API calls
  - Error logging for failures
  - No excessive logging

## Checklist

- [ ] Dashboard loads successfully
- [ ] Summary statistics display correctly
- [ ] Floors table shows all data
- [ ] Floor modal displays configuration
- [ ] Instances table shows running instances
- [ ] Sessions table shows active editors
- [ ] Instance distribution chart renders
- [ ] Most edited floors chart renders
- [ ] Auto-refresh works (30s interval)
- [ ] Manual refresh button works
- [ ] Empty states handled gracefully
- [ ] All API endpoints return valid JSON
- [ ] Input validation prevents attacks
- [ ] Responsive design works on all sizes
- [ ] Compatible with major browsers
- [ ] No performance issues with large datasets
- [ ] No memory leaks on long sessions
- [ ] Error handling works correctly
- [ ] XSS prevention works
- [ ] CORS configured properly
- [ ] Logging is appropriate
